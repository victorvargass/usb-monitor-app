package com.anonymous.usbmonitorapp

import android.media.AudioFormat
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.content.Context
import android.util.Log
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableArray

data class USBDeviceInfo(
    val vendorId: String,
    val productId: String,
    val serialNumber: String?,
    val deviceName: String?,
    val manufacturerName: String?
)

class DualChannelAudioModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    
    private val TAG = "DualChannelAudio"
    
    override fun getName(): String {
        return "DualChannelAudio"
    }
    
    @ReactMethod
    fun getAudioDevices(promise: Promise) {
        try {
            val audioManager = reactApplicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            val deviceArray = Arguments.createArray()
            
            for (device in devices) {
                val deviceInfo = Arguments.createMap()
                deviceInfo.putInt("id", device.id)
                deviceInfo.putString("productName", device.productName.toString())
                deviceInfo.putString("type", getDeviceTypeString(device.type))
                deviceInfo.putBoolean("isSource", device.isSource)
                
                // Información específica para interfaces USB
                if (device.type == AudioDeviceInfo.TYPE_USB_DEVICE || 
                    device.type == AudioDeviceInfo.TYPE_USB_ACCESSORY ||
                    device.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                    deviceInfo.putBoolean("isUSB", true)
                    
                    // Obtener información de canales
                    val channelMasks = device.channelMasks
                    val channelCounts = device.channelCounts
                    
                    val channelInfo = Arguments.createArray()
                    if (channelCounts.isNotEmpty()) {
                        for (count in channelCounts) {
                            channelInfo.pushInt(count)
                        }
                    }
                    deviceInfo.putArray("supportedChannelCounts", channelInfo)
                    
                    // Información adicional de identificación USB
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        // Información de direcciones y puertos USB
                        deviceInfo.putString("address", device.address ?: "N/A")
                        
                        // Información de sampling rates soportados
                        val sampleRates = device.sampleRates
                        val sampleRateArray = Arguments.createArray()
                        if (sampleRates.isNotEmpty()) {
                            for (rate in sampleRates) {
                                sampleRateArray.pushInt(rate)
                            }
                        }
                        deviceInfo.putArray("supportedSampleRates", sampleRateArray)
                        
                        // Información de encodings soportados
                        val encodings = device.encodings
                        val encodingArray = Arguments.createArray()
                        if (encodings.isNotEmpty()) {
                            for (encoding in encodings) {
                                encodingArray.pushInt(encoding)
                            }
                        }
                        deviceInfo.putArray("supportedEncodings", encodingArray)
                    }
                    
                    // Obtener información específica de hardware USB
                    Log.d(TAG, "🔍 Intentando obtener info USB para: ${device.productName}")
                    val usbInfo = getUSBDeviceInfo(device)
                    Log.d(TAG, "📊 Resultado USB info: $usbInfo")
                    
                    if (usbInfo != null) {
                        Log.d(TAG, "✅ Información USB obtenida exitosamente")
                        deviceInfo.putString("vendorId", usbInfo.vendorId)
                        deviceInfo.putString("productId", usbInfo.productId)
                        deviceInfo.putString("serialNumber", usbInfo.serialNumber ?: "N/A")
                        deviceInfo.putString("deviceName", usbInfo.deviceName ?: device.productName.toString())
                        deviceInfo.putString("manufacturerName", usbInfo.manufacturerName ?: "N/A")
                        
                        // Generar identificador único más específico con VID/PID
                        val uniqueId = "USB_${usbInfo.vendorId}_${usbInfo.productId}_${usbInfo.serialNumber ?: device.id.toString()}"
                        deviceInfo.putString("uniqueIdentifier", uniqueId)
                        deviceInfo.putString("hardwareIdentifier", "${usbInfo.vendorId}:${usbInfo.productId}")
                    } else {
                        Log.w(TAG, "⚠️ No se pudo obtener información USB, usando fallback")
                        // Fallback para dispositivos sin información USB detallada
                        val uniqueId = "${device.id}_${device.productName.hashCode()}_${device.type}"
                        deviceInfo.putString("uniqueIdentifier", uniqueId)
                        
                        // Intentar extraer información de la dirección si está disponible
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && device.address != null) {
                            deviceInfo.putString("hardwareIdentifier", "ADDR_${device.address}")
                            Log.d(TAG, "📍 Usando dirección como identificador: ${device.address}")
                        }
                    }
                    deviceInfo.putString("deviceHash", device.productName.hashCode().toString())
                    
                } else {
                    deviceInfo.putBoolean("isUSB", false)
                }
                
                deviceArray.pushMap(deviceInfo)
            }
            
            promise.resolve(deviceArray)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio devices", e)
            promise.reject("DEVICE_ERROR", "Failed to get audio devices: ${e.message}")
        }
    }
    
    private fun getDeviceTypeString(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "BUILTIN_MIC"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB_DEVICE"
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB_ACCESSORY"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BLUETOOTH_SCO"
            else -> "UNKNOWN_$type"
        }
    }
    
    private fun getUSBDeviceInfo(audioDevice: AudioDeviceInfo): USBDeviceInfo? {
        return try {
            Log.d(TAG, "🚀 Iniciando búsqueda de información USB...")
            val usbManager = reactApplicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
            
            if (usbManager == null) {
                Log.e(TAG, "❌ UsbManager es null")
                return null
            }
            
            val deviceList = usbManager.deviceList
            
            // Buscar el dispositivo USB correspondiente por nombre de producto
            val audioProductName = audioDevice.productName.toString()
            Log.d(TAG, "🔍 Buscando dispositivo USB para AudioDevice: '$audioProductName'")
            Log.d(TAG, "🔍 Total dispositivos USB encontrados: ${deviceList.size}")
            
            if (deviceList.isEmpty()) {
                Log.w(TAG, "⚠️ No hay dispositivos USB detectados por UsbManager")
                return null
            }
            
            // Listar todos los dispositivos USB para diagnóstico
            deviceList.values.forEachIndexed { index, usbDevice ->
                val usbProductName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    usbDevice.productName ?: "Sin nombre"
                } else {
                    "API < 21"
                }
                val manufacturerName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    usbDevice.manufacturerName ?: "Sin fabricante"
                } else {
                    "API < 21"
                }
                
                Log.d(TAG, "📱 USB Device #$index:")
                Log.d(TAG, "   📝 Nombre: '$usbProductName'")
                Log.d(TAG, "   🏭 Fabricante: '$manufacturerName'")
                Log.d(TAG, "   🔧 VID: ${String.format("%04X", usbDevice.vendorId)} (${usbDevice.vendorId})")
                Log.d(TAG, "   📦 PID: ${String.format("%04X", usbDevice.productId)} (${usbDevice.productId})")
                Log.d(TAG, "   📂 Clase: ${usbDevice.deviceClass}")
                Log.d(TAG, "   🔌 Interface count: ${usbDevice.interfaceCount}")
            }
            
            // Buscar coincidencias más amplias
            for (usbDevice in deviceList.values) {
                val usbProductName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    usbDevice.productName ?: ""
                } else {
                    ""
                }
                val usbManufacturerName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    usbDevice.manufacturerName ?: ""
                } else {
                    ""
                }
                
                Log.d(TAG, "🔍 Comparando:")
                Log.d(TAG, "   Audio: '$audioProductName'")
                Log.d(TAG, "   USB: '$usbProductName'")
                
                // Múltiples estrategias de matching
                val isMatch = when {
                    // Matching directo por nombre
                    usbProductName.isNotEmpty() && audioProductName.contains(usbProductName, ignoreCase = true) -> {
                        Log.d(TAG, "✅ Match por nombre de producto")
                        true
                    }
                    // Matching por "USB-Audio" en el nombre
                    audioProductName.contains("USB-Audio", ignoreCase = true) && 
                    usbDevice.deviceClass == 1 -> { // Clase 1 = Audio
                        Log.d(TAG, "✅ Match por USB-Audio + clase Audio")
                        true
                    }
                    // Si solo hay un dispositivo USB, asumimos que es el correcto
                    deviceList.size == 1 && usbDevice.deviceClass == 1 -> {
                        Log.d(TAG, "✅ Match por único dispositivo USB de audio")
                        true
                    }
                    else -> {
                        Log.d(TAG, "❌ No match")
                        false
                    }
                }
                
                if (isMatch) {
                    val vendorId = String.format("%04X", usbDevice.vendorId)
                    val productId = String.format("%04X", usbDevice.productId)
                    val serialNumber = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        usbDevice.serialNumber
                    } else {
                        null
                    }
                    val manufacturerName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        usbDevice.manufacturerName
                    } else {
                        null
                    }
                    
                    Log.d(TAG, "🎉 Dispositivo USB encontrado:")
                    Log.d(TAG, "   🔧 VID: $vendorId")
                    Log.d(TAG, "   📦 PID: $productId")
                    Log.d(TAG, "   🔢 Serial: $serialNumber")
                    Log.d(TAG, "   🏢 Fabricante: $manufacturerName")
                    
                    return USBDeviceInfo(
                        vendorId = vendorId,
                        productId = productId,
                        serialNumber = serialNumber,
                        deviceName = usbProductName.ifEmpty { null },
                        manufacturerName = manufacturerName
                    )
                }
            }
            
            Log.w(TAG, "⚠️ No se encontró información USB específica para: $audioProductName")
            null
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error obteniendo información del dispositivo USB", e)
            null
        }
    }
}
