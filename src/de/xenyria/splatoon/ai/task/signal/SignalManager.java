package de.xenyria.splatoon.ai.task.signal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SignalManager {

    public void reset() {

        signalTicks.clear();

    }

    public void tick() {

        Iterator<Map.Entry<SignalType, Integer>> entryIterator = signalTicks.entrySet().iterator();
        while (entryIterator.hasNext()) {

            Map.Entry<SignalType, Integer> entry = entryIterator.next();
            int newVal = entry.getValue()-1;

            if(newVal < 1) {

                entryIterator.remove();

            } else {

                entry.setValue(newVal);

            }

        }

    }
    public void signal(SignalType type, int ticks) {

        signalTicks.put(type, ticks);

    }
    public boolean isActive(SignalType type) {

        return signalTicks.containsKey(type);

    }

    private HashMap<SignalType, Integer> signalTicks = new HashMap<>();
    public void dismiss(SignalType type) {

        if(signalTicks.containsKey(type)) {

            signalTicks.remove(type);

        }

    }

}
