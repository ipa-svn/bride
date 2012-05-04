/**
 * Copyright (c) 2012 Luca Gherardi
 * 
 */
package org.best_of_robotics.transform.orocos.to.cplusplus.handlers;

import java.util.ArrayList;

import org.best_of_robotics.model.orocos.diagram.part.OrocosDiagramEditor;
import org.best_of_robotics.transform.orocos.to.cplusplus.jobs.RosMsgsOperationJob;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.gmf.runtime.emf.core.resources.GMFResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

/**
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class RosMsgsTransformHandler extends AbstractHandler {

	private OrocosDiagramEditor orocosDiagramEdtior;
	private IProject project;
	private RosMsgsOperationJob eglJob;
	private ArrayList<IMarker> problems = new ArrayList<IMarker>();

	public RosMsgsTransformHandler() {
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (!(editor instanceof OrocosDiagramEditor)) {
			MessageDialog
					.openError(
							PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getShell(),
							"Error on Editor Selection",
							"Please select the editor from which you want to generate code and execute command again.");
			return null;
		} else {
			orocosDiagramEdtior = (OrocosDiagramEditor) editor;
		}

		String commandName = "rosMsgsHandler";
//		try {
//			commandName = event.getCommand().getName();
//		} catch (NotDefinedException e) {
//			e.printStackTrace();
//		}

		Resource resource = getFirstSemanticModelResource(orocosDiagramEdtior
				.getEditingDomain().getResourceSet());

		if (!check(resource)) {
			Shell shell = PlatformUI.getWorkbench().getDisplay()
			.getActiveShell();
			MessageDialog
					.openError(
							shell,
							"Model Not Valid Error",
							"The are problems with model. Please check the Problem View for more information.");
			try {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage()
						.showView("org.eclipse.ui.views.ProblemView").setFocus();
			} catch (PartInitException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		eglJob = new RosMsgsOperationJob(commandName);
		
		initializeJobs(resource);

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IResourceRuleFactory ruleFactory = workspace.getRuleFactory();
		ISchedulingRule rule = ruleFactory.modifyRule(project);

		eglJob.setUser(true);
		eglJob.setPriority(Job.INTERACTIVE);
		eglJob.setRule(rule);
		eglJob.schedule();

		return null;
	}

	private void initializeJobs(Resource resource) {
		FileEditorInput fileEditorInput = (FileEditorInput) orocosDiagramEdtior
				.getEditorInput();
		project = fileEditorInput.getFile().getProject();
		eglJob.setProject(project);
		eglJob.createSource(resource);
	}

	private boolean check(Resource resource) {
		if (resource == null) return false;
		org.best_of_robotics.model.orocos.Package rttPackage = (org.best_of_robotics.model.orocos.Package) resource.getContents().get(0);
		for (IMarker marker : problems) {
			try {
				marker.delete();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		problems.clear();
		if (rttPackage.getName() == null || rttPackage.getName().equals("")) {
			problems.add(createProblemMarker("Package name is not set in diagram."));
		}
		if (rttPackage.getTypesLibrary() == null) {
			problems.add(createProblemMarker("Types Library is not set in diagram."));
			
		}else if (rttPackage.getTypesLibrary().getName() == null || rttPackage.getTypesLibrary().getName().equals("")) {
			problems.add(createProblemMarker("Types Library name is not set in data types tree."));
		}
		
		if (!problems.isEmpty()) return false;
		return true;
	}

	private IMarker createProblemMarker(String message) {
		try {
			IMarker problemMarker = ResourcesPlugin.getWorkspace().getRoot().createMarker(IMarker.PROBLEM);
			problemMarker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			problemMarker.setAttribute(IMarker.MESSAGE, message);
			problemMarker.setAttribute(IMarker.TRANSIENT, true);
			problemMarker.setAttribute(IMarker.LOCATION, "RTT Graphical Editor");
			return problemMarker;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
		
	}

	public Resource getFirstSemanticModelResource(ResourceSet resourceSet) {
		for (Resource resource : resourceSet.getResources()) {
			if (!(resource instanceof GMFResource)) {
				return resource;
			}
		}
		return null;
	}

}
