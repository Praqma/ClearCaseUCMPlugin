package net.praqma.hudson.test.integration.userstories;

import net.praqma.hudson.test.BaseTestClass;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.TestDescription;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollSiblingMode;


import static org.junit.Assert.*;

public class Story07IT extends BaseTestClass {
	
	@Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "ccucm-story07", "setup-story7.xml" );
        
	@Test
	@ClearCaseUniqueVobName( name = "story07" )
	@TestDescription( title = "Story 07", text = "New baseline, bl1, on int stream, poll on siblings, no interproject delivers" )
	public void story07() throws Exception {
		
		Stream one = ccenv.context.streams.get( "one_int" );
		Stream two = ccenv.context.streams.get( "two_int" );
		one.setDefaultTarget( two );
        PollSiblingMode mode = new PollSiblingMode("INITIAL");
        mode.setCreateBaseline(false);
        mode.setUseHyperLinkForPolling(false);
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), mode, "_System@" + ccenv.getPVob(), "two_int@" + ccenv.getPVob(), false, false, false, false, true );

		/* Build validation */
		assertTrue( build.getResult().isBetterOrEqualTo( Result.FAILURE ) );
		
		Baseline b = ccenv.context.baselines.get( "model-1" ).load();
		
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.FAILURE )
		.validateBuiltBaseline( PromotionLevel.REJECTED, b, false )
		.validateCreatedBaseline( false )
		.validate();
	}
	
}
