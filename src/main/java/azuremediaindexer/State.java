package azuremediaindexer;

public class State {
    private String value;
    private int progress;

    public State() {
        this.value = "";
        this.progress = 0;
   }

    public void setValue(String value) {
        this.value = value;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getValue() {
        if (this.value == null) {
            this.value = "";
        }
        return this.value;
    }

    public int getProgress() {
        return this.progress;
    }
}
