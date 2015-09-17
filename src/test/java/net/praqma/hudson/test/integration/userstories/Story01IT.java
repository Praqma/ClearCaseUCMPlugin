package net.praqma.hudson.test.integration.userstories;

import net.praqma.hudson.test.BaseTestClass;
import net.praqma.util.test.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.scm.PollingResult;
import net.praqma.hudson.scm.CCUCMScm;
import net.praqma.hudson.test.SystemValidator;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollSelfMode;


import static org.junit.Assert.*;

public class Story01IT extends BaseTestClass {
	
	@Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "ccucm-story01" );

	@Test
	@ClearCaseUniqueVobName( name = "story01b" )
	@TestDescription( title = "Story 01b", text = "New baseline, bl1, on integration stream, poll on self, wrong component." )
	public void story01b() throws Exception {
		
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), new PollSelfMode("INITIAL"), "_System2@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, false );

		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.FAILURE )
		.validateBuiltBaselineNotFound()
		.validate();
	}
	
	@Test
	@ClearCaseUniqueVobName( name = "story01d" )
	@TestDescription( title = "Story 01d, polling", text = "New baseline, bl1, on integration stream, poll on self, wrong component." )
	public void story01d() throws Exception {
		
		/* First build must succeed to get a workspace */
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), new PollSelfMode("INITIAL"), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, false );
		AbstractProject<?, ?> project = build.getProject();
		assertTrue( build.getResult().isBetterOrEqualTo( Result.SUCCESS ) );		
		
		/* New scm with wrong component */
		CCUCMScm scm = new CCUCMScm( "_System2@" + ccenv.getPVob(), "ALL", false, new PollSelfMode("INITIAL"), "one_int@" + ccenv.getPVob(), "successful","", true, false, false, false, "jenkins", true, false, false);
		project.setScm( scm );
		
		TaskListener listener = jenkins.createTaskListener();
		PollingResult result = project.poll( listener );

		/* Polling result validation */
		assertEquals( PollingResult.NO_CHANGES, result );
		
	}
		
}
