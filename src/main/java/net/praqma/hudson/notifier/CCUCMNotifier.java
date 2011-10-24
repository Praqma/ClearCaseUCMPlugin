package net.praqma.hudson.notifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import jcifs.dcerpc.msrpc.netdfs;

import org.kohsuke.stapler.StaplerRequest;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.Cool;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.hudson.Config;
import net.praqma.hudson.exception.NotifierException;
import net.praqma.hudson.nametemplates.NameTemplate;
import net.praqma.hudson.remoting.RemoteDeliverComplete;
import net.praqma.hudson.remoting.RemoteUtil;
import net.praqma.hudson.scm.CCUCMScm;
import net.praqma.hudson.scm.CCUCMState.State;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.appenders.FileAppender;
import net.praqma.util.debug.appenders.StreamAppender;
import net.sf.json.JSONObject;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.Future;
import hudson.remoting.Pipe;
import hudson.remoting.RemoteInputStream;
import hudson.remoting.VirtualChannel;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

/**
 * CCUCMNotifier perfoms the user-chosen CCUCM post-build actions
 *
 * @author Troels Selch
 * @author Margit Bennetzen
 *
 */
public class CCUCMNotifier extends Notifier {

    private PrintStream hudsonOut;

    private Status status;
    private String id = "";
    private Logger logger = null;
    private String jobName = "";
    private Integer jobNumber = 0;
    
    private SimpleDateFormat logformat  = new SimpleDateFormat( "yyyyMMdd-HHmmss" );

    public CCUCMNotifier() {
    }
    
    /**
     * This constructor is used in the inner class <code>DescriptorImpl</code>.
     *
     * @param promote <ol start="0"><li>Baseline will not be promoted after the build</li>
     * <li>Baseline will be promoted after the build if stable</li>
     * <li>Baseline will be promoted after the build if unstable</li></ol>
     * @param recommended
     *            if <code>true</code>, the baseline will be marked
     *            'recommended' in ClearCase.
     * @param makeTag
     *            if <code>true</code>, CCUCM will set a Tag() on the baseline in
     *            ClearCase.
     * @param ucmDeliver The special deliver object, in which all the deliver parameters are encapsulated.

     */
    public CCUCMNotifier( boolean recommended, boolean makeTag, boolean setDescription ) {

    }

    /**
     * This indicates whether to let CCUCM run after(true) the job is done or before(false)
     */
    @Override
    public boolean needsToRunAfterFinalized() {
        return false;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        
        boolean result = true;
        hudsonOut = listener.getLogger();
        
        status = new Status();
        
        this.id = "[" + jobName + "::" + jobNumber + "]";
        
        /* Preparing the logger */
        File logfile = new File( build.getRootDir(), "ccucm.log" );
    	logger = Logger.getLogger();
    	FileAppender app = new FileAppender( logfile );
    	app.setTag( id );
    	net.praqma.hudson.Util.initializeAppender( build, app );
	    Logger.addAppender( app );

        /* Prepare job variables */
        jobName = build.getParent().getDisplayName().replace(' ', '_');
        jobNumber = build.getNumber();





        SCM scmTemp = null;
        /*TODO result is always true!!!!...
         * so we could move the if block unless it should not always be true
         */
        if (result) {
            scmTemp = build.getProject().getScm();
            if (!(scmTemp instanceof CCUCMScm)) {
                listener.fatalError("[" + Config.nameShort + "] Not a CCUCM scm. This Post build action can only be used when polling from ClearCase with CCUCM plugin.");
                result = false;
            }
            scmTemp.toString();
        }

        State pstate = null;
        Baseline baseline = null;
        
        /* Only do this part if a valid CCUCMScm build */
        if (result) {
            /* Retrieve the CCUCM state */
            pstate = CCUCMScm.ccucm.getState(jobName, jobNumber);
            
            /* Validate the state */
            if (pstate.doPostBuild() && pstate.getBaseline() != null) {
                logger.debug(id + "Post build", id);

                /* This shouldn't actually be necessary!?
                 * TODO Maybe the baseline should be re-Load()ed instead of creating a new object?  */
                String bl = pstate.getBaseline().getFullyQualifiedName();

                /* If no baselines found bl will be null and the post build section will not proceed */
                if (bl != null) {
                    try {
                        baseline = UCMEntity.getBaseline(bl);
                    } catch (UCMException e) {
                        logger.warning(id + "Could not initialize baseline.", id);
                        baseline = null;
                    }

                    if (baseline == null) {
                        /* If baseline is null, the user has already been notified in Console output from CCUCMScm.checkout() */
                        result = false;
                    }
                } else {
                    result = false;
                }

            } else {
                // Not performing any post build actions.
                result = false;
            }
        }

        /* There's a valid baseline, lets process it */
        if(result) {

        	status.setErrorMessage( pstate.getError() );
        	
            try {
                processBuild(build, launcher, listener, pstate);
                if (pstate.isSetDescription()) {
                    String d = build.getDescription();
                    if (d != null) {
                        build.setDescription((d.length() > 0 ? d + "<br/>" : "") + status.getBuildDescr());
                    } else {
                    	build.setDescription(status.getBuildDescr());
                    }
                    
                }

            } catch (NotifierException ne) {
                hudsonOut.println(ne.getMessage());
            } catch (IOException e) {
                hudsonOut.println("[" + Config.nameShort + "] Couldn't set build description.");
            }
        } else {
            String d = build.getDescription();
            if (d != null) {
                build.setDescription((d.length() > 0 ? d + "<br/>" : "") + "Nothing to do");
            } else {
                build.setDescription("Nothing to do");
            }

            build.setResult(Result.NOT_BUILT);
        }

        /*
         * Removing baseline and job from collection, do this no matter what as
         * long as the SCM is CCUCM
         */
        if ((scmTemp instanceof CCUCMScm) && baseline != null) {
            boolean done2 = pstate.remove();
            logger.debug(id + "Removing job " + build.getNumber() + " from collection: " + done2, id);
        }
        
        hudsonOut.println( "[" + Config.nameShort + "] Post build steps done" );

        Logger.removeAppender( app );
        return result;
    }

    /**
     * This is where all the meat is. When the baseline is validated, the actual post build steps are performed. <br>
     * First the baseline is delivered(if chosen), then tagged, promoted and recommended.
     * @param build The build object in which the post build action is selected
     * @param launcher The launcher of the build
     * @param listener The listener of the build
     * @param pstate The {@link CCUCMState} of the build.
     * @throws NotifierException
     */
    private void processBuild(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, State pstate) throws NotifierException {
        Result buildResult = build.getResult();

        VirtualChannel ch = launcher.getChannel();

        if (ch == null) {
            logger.debug("The channel was null", id);
        }

        FilePath workspace = build.getExecutor().getCurrentWorkspace();

        if (workspace == null) {
            logger.warning("Workspace is null", id);
            throw new NotifierException("Workspace is null");
        }

        hudsonOut.println("[" + Config.nameShort + "] Build result: " + buildResult);
        
        /* Initialize variables for post build steps */
        Stream targetstream = null;
		try {
			targetstream = pstate.getBaseline().getStream();
		} catch (UCMException e2) {
			logger.error( "The target stream could not be resolved: " + e2.getMessage(), id );
		}
        Stream sourcestream = targetstream;
        Baseline sourcebaseline = pstate.getBaseline();
        Baseline targetbaseline = sourcebaseline;

        logger.debug( "NTBC: " + pstate.needsToBeCompleted(), id );
        

        /* Determine whether to treat the build as successful
         * 
         * Success:
         *  - deliver complete
         *  - create baseline
         *  - recommend
         *  
         * Fail:
         *  - deliver cancel
         * 
         *  */
        boolean treatSuccessful = buildResult.isBetterThan( pstate.getUnstable().treatSuccessful() ? Result.FAILURE : Result.UNSTABLE );
        
        /* Finalize CCUCM, deliver + baseline 
         * Only do this for child and sibling polling */
        if( pstate.needsToBeCompleted() && pstate.getPolling().isPollingOther() ) {
            status.setBuildStatus(buildResult);
                        
            try {                
                hudsonOut.print("[" + Config.nameShort + "] " + ( treatSuccessful ? "Completing" : "Cancelling" ) + " the deliver. ");
                RemoteUtil.completeRemoteDeliver( workspace, listener, pstate, treatSuccessful );
                hudsonOut.println("Success.");
                
                /* If deliver was completed, create the baseline */
                if( treatSuccessful && pstate.createBaseline() ) {

                	try {
                        hudsonOut.print("[" + Config.nameShort + "] Creating baseline on Integration stream. ");
                        pstate.setWorkspace( workspace );
                        NameTemplate.validateTemplates( pstate );
                        String name = NameTemplate.parseTemplate( pstate.getNameTemplate(), pstate );
                        
                        targetbaseline = RemoteUtil.createRemoteBaseline( workspace, listener, name, pstate.getBaseline().getComponent(), pstate.getSnapView().getViewRoot(), pstate.getBaseline().getUser() );
                        
                        hudsonOut.println( targetbaseline );
                    } catch( Exception e ) {
                        hudsonOut.println(" Failed: " + e.getMessage());
                        logger.warning( "Failed to create baseline on stream", id );
                        logger.warning( e, id );
                        /* We cannot recommend a baseline that is not created */
                        if( pstate.doRecommend() ) {
                        	hudsonOut.println( "[" + Config.nameShort + "] Cannot recommend Baseline when not created" );
                        }
                        
                        pstate.setRecommend( false );
                    }
                }
                
            } catch( Exception e ) {
                status.setBuildStatus(buildResult);
                status.setStable(false);
                hudsonOut.println("Failed.");
                logger.warning(e, id);
                
                /* We cannot recommend a baseline that is not created */
                if( pstate.doRecommend() ) {
                	hudsonOut.println( "[" + Config.nameShort + "] Cannot recommend a baseline when deliver failed" );
                }
                pstate.setRecommend( false );
                
                /* If trying to complete and it failed, try to cancel it */
                if( treatSuccessful ) {
                    try{
                        hudsonOut.print("[" + Config.nameShort + "] Trying to cancel the deliver. ");
                        RemoteUtil.completeRemoteDeliver( workspace, listener, pstate, false );
                        hudsonOut.println("Success.");
                    } catch( Exception e1 ) {
                        hudsonOut.println(" Failed.");
                        logger.warning( "Failed to cancel deliver", id );
                        logger.warning( e, id );
                    }
                } else {
                    logger.warning( "Failed to cancel deliver", id );
                    logger.warning( e, id );
                }
            }
        } 
        
        
        if( pstate.getPolling().isPollingOther() ) {
        	targetstream = pstate.getStream();
        }
    
        /* Remote post build step, common to all types */
        try {
            logger.debug(id + "Remote post build step", id);
            hudsonOut.println("[" + Config.nameShort + "] Performing common post build steps");

            status = workspace.act( new RemotePostBuild(buildResult, status, listener,
                        pstate.isMakeTag(), pstate.doRecommend(), pstate.getUnstable(), 
                        sourcebaseline, targetbaseline, sourcestream, targetstream, build.getParent().getDisplayName(), Integer.toString(build.getNumber()) ) );
            
        } catch (Exception e) {
            status.setStable(false);
            logger.debug(id + "Something went wrong: " + e.getMessage(), id);
            logger.warning(e, id);
            hudsonOut.println("[" + Config.nameShort + "] Error: Post build failed: " + e.getMessage());
        }

        /* If the promotion level of the baseline was changed on the remote */
        if (status.getPromotedLevel() != null) {
            pstate.getBaseline().setPromotionLevel(status.getPromotedLevel());
            logger.debug(id + "Baselines promotion level sat to " + status.getPromotedLevel().toString(), id);
        }
        
        status.setBuildStatus(buildResult);
        
        if (!status.isStable()) {
            build.setResult(Result.UNSTABLE);
        }
    }

    /**
     * This class is used by Hudson to define the plugin.
     *
     * @author Troels Selch
     * @author Margit Bennetzen
     *
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(CCUCMNotifier.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "ClearCase UCM";
        }

        /**
         * Hudson uses this method to create a new instance of
         * <code>CCUCMNotifier</code>. The method gets information from Hudson
         * config page. This information is about the configuration, which
         * Hudson saves.
         */
        @Override
        public Notifier newInstance(StaplerRequest req, JSONObject formData) throws FormException {

            save();

            return new CCUCMNotifier();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }
    }
}
