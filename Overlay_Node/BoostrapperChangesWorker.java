package Overlay_Node;

public class BoostrapperChangesWorker implements Runnable{
    
    private ONode node;
    private BootstrapperHandler bootstrapperHandler;
    private static long AUTOMATIC_VERIFY_TIME = 10000; 

    public BoostrapperChangesWorker(ONode node, BootstrapperHandler bootstrapperHandler)
    {
        this.node = node;
        this.bootstrapperHandler = bootstrapperHandler;
    }
    
    @Override
    public void run() {

    }
    
}
