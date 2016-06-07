package azuremediaindexer;

import azuremediaindexer.State;

public interface Observer {
    public void notify(State state);
}
