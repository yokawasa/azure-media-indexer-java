package azuremediaindexer;

import azuremediaindexer.Observer;
import azuremediaindexer.State;

public class StateListener implements Observer {
    public String name;
    public State state;

    public StateListener(String name) {
        this.name = name;
        this.state = new State();
    }

    @Override
    public void notify(State state) {
        this.state = state;
    }
}
