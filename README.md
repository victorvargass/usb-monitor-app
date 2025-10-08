# 📱 USB Monitor App

App Android simple para monitorear dispositivos USB de audio conectados en tiempo real.

## 📋 Requisitos

- Node.js 20+
- Android Studio
- Un dispositivo Android físico (recomendado) o emulador
- Cable USB OTG (para conectar dispositivos USB al teléfono)

## 🚀 Instalación

### 1. Instalar dependencias

```bash
npm install
```

### 3. Compilar la app Android

```bash
npm run android
```

Esto compilará el proyecto Android con el módulo nativo y lo instalará en tu dispositivo conectado.

## 📱 Cómo usar

1. **Ejecuta la app** en tu dispositivo Android
2. **Conecta un dispositivo USB** de audio usando un cable OTG
3. **Observa la información** que aparece automáticamente

## 📊 Información Mostrada

Cuando conectas un dispositivo USB, la app muestra:

- 🏷️ **Nombre**: Nombre del producto
- 🆔 **ID Android**: Identificador interno del sistema
- 📱 **Tipo**: Tipo de dispositivo (USB_DEVICE, USB_HEADSET, etc.)
- 🏭 **Vendor ID**: ID del fabricante (formato hexadecimal)
- 📦 **Product ID**: ID del producto (formato hexadecimal)
- 🔢 **Serial**: Número de serie (si está disponible)
- 🏢 **Fabricante**: Nombre del fabricante
- 🔑 **ID Único**: Identificador único generado
- 📍 **Dirección**: Dirección del dispositivo
- 🎵 **Canales soportados**: Cantidad de canales de audio
- 📊 **Sample Rates**: Tasas de muestreo soportadas

## 📄 Licencia

MIT