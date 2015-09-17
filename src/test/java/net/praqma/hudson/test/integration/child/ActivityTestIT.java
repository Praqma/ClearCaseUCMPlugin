package net.praqma.hudson.test.integration.child;

import java.io.File;
import java.util.List;

import net.praqma.hudson.test.BaseTestClass;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.User;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Baseline.LabelBehaviour;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.util.debug.Logger;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollChildMode;

import static org.junit.Assert.*;

public class ActivityTestIT extends BaseTestClass {
	
	@Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "ccucm" );

	private static Logger logger = Logger.getLogger();
		
	public AbstractBuild<?, ?> initiateBuild( String projectName, boolean recommend, boolean tag, boolean description, boolean fail ) throws Exception {
        PollChildMode mode = new PollChildMode("INITIAL");
        mode.setCreateBaseline(false);
		return jenkins.initiateBuild( projectName, mode, "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), recommend, tag, description, fail, true );
	}

	@Test
	@ClearCaseUniqueVobName( name = "activity-chglg" )
	public void activityInChangelog() throws Exception {
		
		Baseline baseline = getNewBaseline();
		
		AbstractBuild<?, ?> build = initiateBuild( "activity-" + ccenv.getUniqueName(), false, false, false, false );

		List<User> users = jenkins.getActivityUsers( build );
		
		logger.debug( "Checking USERS" );
		for( User user : users ) {
			logger.debug( "Checking \"" + user.getDisplayName() + "\"" );
			assertNotNull( user.getDisplayName() );
			assertFalse( user.getDisplayName().matches( "^\\s*$" ) );
		}
	}
		
	protected Baseline getNewBaseline() throws ClearCaseException {
		/**/
		String viewtag = ccenv.getUniqueName() + "_one_dev";
		System.out.println( "VIEW: " + ccenv.context.views.get( viewtag ) );
		File path = new File( ccenv.context.mvfs + "/" + ccenv.getUniqueName() + "_one_dev/" + ccenv.getVobName() );
				
		System.out.println( "PATH: " + path );
		
		Stream stream = Stream.get( "one_dev", ccenv.getPVob() );
		Activity activity = Activity.create( "ccucm-activity", stream, ccenv.getPVob(), true, "ccucm activity", null, path );
		UCMView.setActivity( activity, path, null, null );
		
		try {
			ccenv.addNewElement( ccenv.context.components.get( "Model" ), path, "test2.txt" );
		} catch( ClearCaseException e ) {
			ExceptionUtils.print( e, System.out, true );
		}
		return Baseline.create( "baseline-for-test", ccenv.context.components.get( "_System" ), path, LabelBehaviour.FULL, false );
	}
	
}
