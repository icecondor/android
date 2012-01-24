package com.icecondor.nest;

interface PigeonService {
  boolean isTransmitting();
  void startTransmitting();
  void stopTransmitting();
  Location getLastFix();
  Location getLastPushedFix();
  void refreshRSS();
  void pushFix();
  void followFriend(String username);
  void addFriend(String username);
  void unFriend(String username);
}