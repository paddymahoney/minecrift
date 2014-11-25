package com.mtbs3d.minecrift.api;

import java.util.ArrayList;
import java.util.List;

public interface IEventNotifier {
    List<IEventListener> listeners = new ArrayList<IEventListener>();

    public void registerListener(IEventListener listener);
    public void notifyListeners(int eventId);
}
