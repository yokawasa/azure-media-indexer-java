package azuremediaindexer;

import azuremediaindexer.Observer;
import azuremediaindexer.State;
import java.util.concurrent.CopyOnWriteArrayList;

public class Subject {
    private CopyOnWriteArrayList<Observer> observers = new CopyOnWriteArrayList<>();

    public void addObserver(Observer o) {
        observers.add(o);
    }

    public void deleteObserver(Observer o) {
        observers.remove(o);
    }

    public void notifyObservers(State state) {
        for (Observer o : observers) {
            o.notify(state);
        }
    }
}
