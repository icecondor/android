package com.icecondor.nest;

interface PigeonService {
  boolean isTransmitting();
  void startTransmitting();
  void stopTransmitting();
  Location getLastFix();
}