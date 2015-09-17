package net.praqma.hudson.test.integration.userstories;

import java.io.File;

import net.praqma.hudson.test.BaseTestClass;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Baseline.LabelBehaviour;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.TestDescription;
import net.praqma.util.debug.Logger;

import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollChildMode;


public class Story04IT extends BaseTestClass {
	
	@Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "basic" );

	private static Logger logger = Logger.getLogger();

	@Test
	@ClearCaseUniqueVobName( name = "story04" )
	@TestDescription( title = "Story 04", text = "New baseline, bl1, on dev stream, poll on childs. Merge error on deliver" )
	public void story04() throws Exception {
		
		Stream source = ccenv.context.streams.get( "one_dev" );
		Stream target = ccenv.context.streams.get( "one_int" );
		
		/* Prepare */
		/* Integration */
		String tviewtag = ccenv.getUniqueName() + "_one_int";
		File tpath = ccenv.setDynamicActivity( target, tviewtag, "strict-deliver" );
		Baseline tb = getNewBaseline( tpath, "merge.txt", "one" );
		target.recommendBaseline( tb );
		
		/* Development */
		String viewtag = ccenv.getUniqueName() + "_one_dev";
		File path = ccenv.setDynamicActivity( source, viewtag, "strict-deliver-dev" );
		Baseline b = getNewBaseline( path, "merge.txt", "two" );
		
		
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), new PollChildMode("INITIAL"), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, true );

		Baseline buildBaseline = jenkins.getBuildBaseline( build );
		
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.FAILURE )
		.validateBuiltBaseline( PromotionLevel.REJECTED, buildBaseline, false )
		.validateCreatedBaseline( false )
		.validate();
	}

	
	protected Baseline getNewBaseline( File path, String filename, String bname ) throws ClearCaseException {
		
		try {
			ccenv.addNewElement( ccenv.context.components.get( "Model" ), path, filename );
		} catch( ClearCaseException e ) {
			ExceptionUtils.print( e, System.out, true );
		}
		return Baseline.create( bname, ccenv.context.components.get( "_System" ), path, LabelBehaviour.FULL, false );
	}
}
