package net.praqma.hudson.test.integration.userstories;

import net.praqma.hudson.test.BaseTestClass;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;
import net.praqma.util.debug.Logger;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollChildMode;


public class Story09IT extends BaseTestClass {
	
	@Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "ccucm-story09", "setup-story10.xml" );
	
	@Rule
	public DescriptionRule desc = new DescriptionRule();

	@Test
	@ClearCaseUniqueVobName( name = "story09" )
	@TestDescription( title = "Story 9", text = "New baseline, bl1, on dev stream, dev1, poll on child, don't create baselines" )
	public void story09() throws Exception {
		PollChildMode childMode = new PollChildMode("INITIAL");
  
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), childMode, "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, false );

		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.SUCCESS )
		.validateBuiltBaseline( PromotionLevel.BUILT, baseline, false )
		.validateCreatedBaseline( false )
		.validate();
	}
	
}
