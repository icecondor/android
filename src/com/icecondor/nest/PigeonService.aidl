package com.icecondor.nest;

interface PigeonService {
  boolean isTransmitting();
  void startTransmitting();
  void stopTransmitting();
  Location getLastFix(boolean broadcast);
  Location getLastPushedFix();
  void refreshRSS();
  void pushFix();
  void followFriend(String username);
  void unfollowFriend(String username);
  void addFriend(String username);
  void unFriend(String username);
  void followFriends();
  void unfollowFriends();
}