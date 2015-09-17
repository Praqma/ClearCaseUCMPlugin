package net.praqma.hudson.test.integration.userstories;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.util.test.junit.TestDescription;
import org.junit.Ignore;
import org.junit.Test;

public class Story06WithSlavesIT extends Story06BaseIT {

    @Test
    @ClearCaseUniqueVobName( name = "dip4" )
    @TestDescription( title = "Story 6", text = "New baseline on dev stream. Deliver in progress from another stream, different view", configurations = { "Force deliver = true", "Poll childs", "Slave = true" }	)
    public void story06_4() throws Exception {
        Stream dev1 = ccenv.context.streams.get( "one_dev" );
        Stream dev2 = ccenv.context.streams.get( "two_dev" );
        runWithSlave( dev1, dev2, ccenv.getUniqueName() + "_one_dev", ccenv.getUniqueName() + "_two_dev", false );
    }

    @Test
    @ClearCaseUniqueVobName( name = "dip5" )
    @TestDescription( title = "Story 6", text = "New baseline on dev stream. Deliver in progress from same stream, different view", configurations = { "Force deliver = true", "Poll childs", "Slave = true" }	)
    public void story06_5() throws Exception {
        Stream dev1 = ccenv.context.streams.get( "one_dev" );
        runWithSlave( dev1, dev1, ccenv.getUniqueName() + "_one_dev", ccenv.getUniqueName() + "_one_dev", false );
    }

    //TODO: This, for some reason starts to randomly fail when upgrading from 1.532.3 -> 1.554.3
    @Test
    @Ignore
    @ClearCaseUniqueVobName( name = "dip6" )
    @TestDescription( title = "Story 6", text = "New baseline on dev stream. Deliver in progress from previous build, different view", configurations = { "Force deliver = true", "Poll childs", "Slave = true" }	)
    public void story06_6() throws Exception {
        Stream dev1 = ccenv.context.streams.get( "one_dev" );
        runWithSlave( null, dev1, null, ccenv.getUniqueName() + "_one_dev", true );
    }

}
