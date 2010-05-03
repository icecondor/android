package com.icecondor.nest;

interface PigeonService {
  boolean isTransmitting();
  void startTransmitting();
  void stopTransmitting();
  Location getLastFix();
  Location getLastPushedFix();
  void refreshRSS();
  void pushFix();
}