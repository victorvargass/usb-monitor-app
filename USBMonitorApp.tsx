import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Platform,
  SafeAreaView,
  StatusBar,
  TouchableOpacity,
  Clipboard
} from 'react-native';
import DualChannelAudio, { AudioDevice } from './DualChannelAudio';

/**
 * App simple para monitorear dispositivos USB
 * 
 * - Muestra pantalla de espera cuando no hay dispositivo
 * - Muestra información cuando se conecta
 * - Actualiza cuando cambia el dispositivo
 */
export default function USBMonitorApp() {
  const [connectedDevice, setConnectedDevice] = useState<AudioDevice | null>(null);
  const [isChecking, setIsChecking] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  // Función para copiar al portapapeles
  const copyToClipboard = (text: string) => {
    Clipboard.setString(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000); // Ocultar mensaje después de 2 segundos
  };

  // Verificar dispositivos cada segundo
  useEffect(() => {
    if (Platform.OS !== 'android') {
      setError('Esta app solo funciona en Android');
      setIsChecking(false);
      return;
    }

    const checkDevices = async () => {
      try {
        const devices = await DualChannelAudio.getAudioDevices();
        const usbDevice = devices.find(device => device.isUSB);
        
        // Si hay un dispositivo USB conectado
        if (usbDevice) {
          // Solo actualizar si cambió el dispositivo
          if (!connectedDevice || connectedDevice.id !== usbDevice.id) {
            setConnectedDevice(usbDevice);
          }
        } else {
          // No hay dispositivo USB
          if (connectedDevice) {
            setConnectedDevice(null);
          }
        }
        
        setIsChecking(false);
        setError(null);
      } catch (err) {
        setError('Error al detectar dispositivos');
        setIsChecking(false);
      }
    };

    // Verificar inmediatamente
    checkDevices();

    // Verificar cada segundo
    const interval = setInterval(checkDevices, 1000);

    return () => clearInterval(interval);
  }, [connectedDevice]);

  // Pantalla de error
  if (error) {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="light-content" backgroundColor="#E53935" />
        <View style={styles.errorContainer}>
          <Text style={styles.errorIcon}>⚠️</Text>
          <Text style={styles.errorText}>{error}</Text>
        </View>
      </SafeAreaView>
    );
  }

  // Pantalla de carga inicial
  if (isChecking && !connectedDevice) {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="dark-content" backgroundColor="#F5F7FA" />
        <View style={styles.waitingContainer}>
          <ActivityIndicator size="large" color="#4A90E2" />
          <Text style={styles.waitingText}>Inicializando...</Text>
        </View>
      </SafeAreaView>
    );
  }

  // Pantalla de espera (sin dispositivo)
  if (!connectedDevice) {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="dark-content" backgroundColor="#F5F7FA" />
        <View style={styles.waitingContainer}>
          <Text style={styles.waitingIcon}>🔌</Text>
          <Text style={styles.waitingTitle}>Esperando dispositivo USB</Text>
          <Text style={styles.waitingSubtitle}>
            Conecta un dispositivo de audio USB para ver su información
          </Text>
          <View style={styles.pulseContainer}>
            <ActivityIndicator size="small" color="#999" />
          </View>
        </View>
      </SafeAreaView>
    );
  }

  // Pantalla con información del dispositivo
  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#4CAF50" />
      <View style={styles.header}>
        <Text style={styles.headerIcon}>✅</Text>
        <Text style={styles.headerTitle}>Dispositivo Conectado</Text>
      </View>

      <ScrollView style={styles.scrollView} contentContainerStyle={styles.scrollContent}>
        {/* Mensaje de copiado */}
        {copied && (
          <View style={styles.copiedMessage}>
            <Text style={styles.copiedText}>✓ Copiado</Text>
          </View>
        )}

        <View style={styles.infoCard}>
          <InfoRow 
            icon="🏷️" 
            label="Nombre" 
            value={connectedDevice.productName} 
          />
          
          <InfoRow 
            icon="🆔" 
            label="ID Android" 
            value={String(connectedDevice.id)} 
          />
          
          <InfoRow 
            icon="📱" 
            label="Tipo" 
            value={String(connectedDevice.type)} 
          />
  
          {/* Información específica de hardware USB */}
          {connectedDevice.vendorId && (
            <InfoRow 
              icon="🏭" 
              label="Vendor ID" 
              value={connectedDevice.vendorId} 
            />
          )}

          {connectedDevice.productId && (
            <InfoRow 
              icon="📦" 
              label="Product ID" 
              value={connectedDevice.productId} 
            />
          )}

          {connectedDevice.serialNumber && connectedDevice.serialNumber !== 'N/A' && (
            <InfoRow 
              icon="🔢" 
              label="Serial" 
              value={connectedDevice.serialNumber} 
            />
          )}

          {connectedDevice.manufacturerName && connectedDevice.manufacturerName !== 'N/A' && (
            <InfoRow 
              icon="🏢" 
              label="Fabricante" 
              value={connectedDevice.manufacturerName} 
            />
          )}

          {/* Hash */}
          {connectedDevice.deviceHash && (
            <InfoRow 
              icon="🔑" 
              label="Hash" 
              value={connectedDevice.deviceHash}
              onPress={() => copyToClipboard(connectedDevice.deviceHash || '')}
            />
          )}

          {/* Capacidades técnicas */}
          {connectedDevice.supportedChannelCounts && connectedDevice.supportedChannelCounts.length > 0 && (
            <InfoRow 
              icon="🎵" 
              label="Canales soportados" 
              value={connectedDevice.supportedChannelCounts.join(', ')} 
            />
          )}

          {connectedDevice.supportedSampleRates && connectedDevice.supportedSampleRates.length > 0 && (
            <InfoRow 
              icon="📊" 
              label="Sample Rates" 
              value={
                connectedDevice.supportedSampleRates.length > 5
                  ? `${connectedDevice.supportedSampleRates.slice(0, 5).join(', ')}...`
                  : connectedDevice.supportedSampleRates.join(', ')
              } 
            />
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

// Componente auxiliar para mostrar filas de información
function InfoRow({ icon, label, value, onPress }: { 
  icon: string; 
  label: string; 
  value: string; 
  onPress?: () => void 
}) {
  const content = (
    <>
      <View style={styles.infoLabel}>
        <Text style={styles.infoIcon}>{icon}</Text>
        <Text style={styles.infoLabelText}>{label}</Text>
      </View>
      <Text style={[styles.infoValue, onPress && styles.clickableValue]}>{value}</Text>
    </>
  );

  if (onPress) {
    return (
      <TouchableOpacity style={styles.infoRow} onPress={onPress} activeOpacity={0.7}>
        {content}
      </TouchableOpacity>
    );
  }

  return (
    <View style={styles.infoRow}>
      {content}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F7FA',
  },
  
  // Header cuando hay dispositivo conectado
  header: {
    backgroundColor: '#4CAF50',
    paddingTop: 60,
    paddingBottom: 20,
    paddingHorizontal: 20,
    alignItems: 'center',
  },
  headerIcon: {
    fontSize: 40,
    marginBottom: 8,
  },
  headerTitle: {
    fontSize: 22,
    fontWeight: 'bold',
    color: 'white',
  },

  // Pantalla de espera
  waitingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 40,
  },
  waitingIcon: {
    fontSize: 80,
    marginBottom: 20,
  },
  waitingTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
    textAlign: 'center',
  },
  waitingSubtitle: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
    lineHeight: 24,
  },
  pulseContainer: {
    marginTop: 30,
  },

  // Pantalla de error
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 40,
  },
  errorIcon: {
    fontSize: 60,
    marginBottom: 20,
  },
  errorText: {
    fontSize: 18,
    color: '#E53935',
    textAlign: 'center',
  },

  // Contenido con información del dispositivo
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    padding: 20,
  },
  infoCard: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  infoLabel: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  infoIcon: {
    fontSize: 20,
    marginRight: 10,
  },
  infoLabelText: {
    fontSize: 14,
    color: '#666',
    fontWeight: '600',
  },
  infoValue: {
    fontSize: 14,
    color: '#333',
    fontWeight: '500',
    flex: 1,
    textAlign: 'right',
    marginLeft: 16,
  },
  waitingText: {
    fontSize: 16,
    color: '#666',
    marginTop: 16,
  },

  // Mensaje de copiado
  copiedMessage: {
    backgroundColor: '#4CAF50',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
    alignSelf: 'center',
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
    elevation: 4,
  },
  copiedText: {
    color: 'white',
    fontSize: 14,
    fontWeight: '600',
  },

  // Valor clickeable
  clickableValue: {
    color: '#4A90E2',
  },
});
