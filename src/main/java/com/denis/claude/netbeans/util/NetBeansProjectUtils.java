package com.denis.claude.netbeans.util;

import java.io.File;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.windows.TopComponent;

/**
 * Utilitaires pour récupérer les informations du projet NetBeans.
 */
public class NetBeansProjectUtils {

    /**
     * Récupère le répertoire du projet actif dans NetBeans.
     * Essaie dans l'ordre:
     * 1. Le projet du fichier sélectionné dans l'explorateur
     * 2. Le projet principal (main project)
     * 3. Le premier projet ouvert
     * 4. Le répertoire home de l'utilisateur en fallback
     */
    public static File getActiveProjectDirectory() {
        // 1. Essayer d'obtenir le projet depuis la sélection actuelle
        Project selectedProject = getProjectFromSelection();
        if (selectedProject != null) {
            File dir = getProjectDir(selectedProject);
            if (dir != null) {
                return dir;
            }
        }

        // 2. Essayer le projet principal
        Project mainProject = OpenProjects.getDefault().getMainProject();
        if (mainProject != null) {
            File dir = getProjectDir(mainProject);
            if (dir != null) {
                return dir;
            }
        }

        // 3. Essayer le premier projet ouvert
        Project[] openProjects = OpenProjects.getDefault().getOpenProjects();
        if (openProjects.length > 0) {
            File dir = getProjectDir(openProjects[0]);
            if (dir != null) {
                return dir;
            }
        }

        // 4. Fallback: répertoire home
        return new File(System.getProperty("user.home"));
    }

    /**
     * Récupère le projet depuis la sélection actuelle dans l'explorateur.
     */
    private static Project getProjectFromSelection() {
        try {
            // Obtenir le TopComponent actif (explorateur de fichiers, etc.)
            TopComponent tc = TopComponent.getRegistry().getActivated();
            if (tc == null) {
                return null;
            }

            // Obtenir les nœuds sélectionnés
            Node[] nodes = tc.getActivatedNodes();
            if (nodes == null || nodes.length == 0) {
                return null;
            }

            // Chercher un projet dans le premier nœud sélectionné
            Node node = nodes[0];

            // Essayer d'obtenir un DataObject depuis le nœud
            DataObject dataObject = node.getLookup().lookup(DataObject.class);
            if (dataObject != null) {
                FileObject fo = dataObject.getPrimaryFile();
                if (fo != null) {
                    return FileOwnerQuery.getOwner(fo);
                }
            }

            // Essayer d'obtenir un Project directement
            Project project = node.getLookup().lookup(Project.class);
            if (project != null) {
                return project;
            }

            // Essayer d'obtenir un FileObject directement
            FileObject fo = node.getLookup().lookup(FileObject.class);
            if (fo != null) {
                return FileOwnerQuery.getOwner(fo);
            }

        } catch (Exception e) {
            // Ignorer les erreurs et continuer avec les fallbacks
        }
        return null;
    }

    /**
     * Convertit un Project en File représentant son répertoire.
     */
    private static File getProjectDir(Project project) {
        if (project == null) {
            return null;
        }
        FileObject projectDir = project.getProjectDirectory();
        if (projectDir != null) {
            return FileUtil.toFile(projectDir);
        }
        return null;
    }

    /**
     * Récupère le nom du projet actif.
     */
    public static String getActiveProjectName() {
        Project project = getProjectFromSelection();
        if (project == null) {
            project = OpenProjects.getDefault().getMainProject();
        }
        if (project == null) {
            Project[] openProjects = OpenProjects.getDefault().getOpenProjects();
            if (openProjects.length > 0) {
                project = openProjects[0];
            }
        }
        if (project != null) {
            return org.netbeans.api.project.ProjectUtils.getInformation(project).getDisplayName();
        }
        return "Aucun projet";
    }
}
