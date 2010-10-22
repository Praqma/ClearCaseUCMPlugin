package net.praqma;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

public class ChangeLogEntryImpl extends Entry {
	
	private ChangeLogSetImpl parent; //Try to delete this and save to super.parent.
	private String filepath;
	private String comment;
	protected static Debug logger = Debug.GetLogger();
	private volatile List<String> affectedPaths = new ArrayList<String>(); //TODO: Find out what this is
	
	public ChangeLogEntryImpl(){
		logger.trace_function();
	}

	@Override
	public Collection<String> getAffectedPaths() {
		logger.trace_function();
		if(affectedPaths.size()==0){
			logger.log("affectedPaths er tom");
			affectedPaths.add("Dummydata");//DELETE AFTER DEBUG
		}
		return affectedPaths;
	}
	
	public void setNextFilepath(String filepath){
		logger.trace_function();
		if(filepath == null)
			logger.log("Filepath er null"+filepath);
		affectedPaths.add(filepath);
	}

	@Override
	public User getAuthor() {
		// TODO Auto-generated method stub
		logger.trace_function();
		User u = User.getUnknown();
		//logger.log(" Unknown user: "+u.toString());
		return u;
	}

	public String getFilepath() {
		// TODO Auto-generated method stub
		logger.trace_function();
		return filepath;
	}
	
	public void setParent(ChangeLogSetImpl parent){
		logger.trace_function();
		this.parent = parent;
	}

	@Override
	public String getMsg() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getComment() {
		return comment;
	}
	
	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}
}