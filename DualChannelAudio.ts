import { NativeModules } from 'react-native';

export interface AudioDevice {
  id: number;
  productName: string;
  type: string;
  isSource: boolean;
  isUSB: boolean;
  supportedChannelCounts?: number[];
  address?: string;
  supportedSampleRates?: number[];
  supportedEncodings?: number[];
  uniqueIdentifier?: string;
  deviceHash?: string;
  // Información específica de hardware USB
  vendorId?: string;
  productId?: string;
  serialNumber?: string;
  deviceName?: string;
  manufacturerName?: string;
  hardwareIdentifier?: string;
}

export interface DualChannelAudioModule {
  getAudioDevices(): Promise<AudioDevice[]>;
}

const { DualChannelAudio } = NativeModules;

export default DualChannelAudio as DualChannelAudioModule;
