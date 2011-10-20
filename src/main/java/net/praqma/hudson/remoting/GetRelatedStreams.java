package net.praqma.hudson.remoting;

import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import net.praqma.clearcase.Cool;
import net.praqma.clearcase.Cool.ContextType;
import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Stream;

public class GetRelatedStreams implements FileCallable<List<Stream>> {

	private static final long serialVersionUID = -8984877325832486334L;

	private Stream stream;
	private boolean pollingChildStreams;
	private TaskListener listener;
	
	public GetRelatedStreams( TaskListener listener, Stream stream, boolean pollingChildStreams ) {
		this.stream = stream;
		this.pollingChildStreams = pollingChildStreams;
		this.listener = listener;
    }
    
    @Override
    public List<Stream> invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
    	
    	PrintStream out = listener.getLogger();
    	
    	Cool.setContext( ContextType.CLEARTOOL );
    	
    	List<Stream> streams = null;
    	
    	try {
        	if( pollingChildStreams ) {
        		streams = stream.getChildStreams();
        	} else {
        		streams = stream.getSiblingStreams();
        	}
        } catch( UCMException e1 ) {
        	e1.printStackTrace( out );
        	throw new IOException( "Could not find any related streams: " + e1.getMessage() );
        }
        
        return streams;
    }
}
