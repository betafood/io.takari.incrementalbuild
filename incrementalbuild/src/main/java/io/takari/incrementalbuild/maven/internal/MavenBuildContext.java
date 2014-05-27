package io.takari.incrementalbuild.maven.internal;

import io.takari.incrementalbuild.spi.DefaultBuildContext;
import io.takari.incrementalbuild.workspace.MessageSink;

import java.io.IOException;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.execution.scope.WeakMojoExecutionListener;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Maven specific BuildContext implementation that provides
 * <ul>
 * <li>Conventional location of incremental build state under ${build.build.directory}/incremental.
 * In the future, this may become configurable via well-known project property.</li>
 * <li>Automatic detection of configuration changes based on
 * <ul>
 * <li>Maven plugin artifacts GAVs, file sizes and timestamps</li>
 * <li>Project effective pom.xml. In the future, this may be narrowed down.</li>
 * <li>Maven session execution, i.e. user and system, properties.</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @TODO decide how to handle volatile properties like ${maven.build.timestamp}. Should we always
 *       ignore them? Are there cases where output has to be always regenerated just to include new
 *       build timestamp, for example?
 */
@Named
@Typed({DefaultBuildContext.class, MavenBuildContext.class})
@MojoExecutionScoped
public class MavenBuildContext extends DefaultBuildContext<MojoExecutionException>
    implements
      WeakMojoExecutionListener {

  @Inject
  public MavenBuildContext(ProjectWorkspace workspace, MessageSink messageSink,
      MojoConfigurationDigester digester, MavenIncrementalConventions conventions,
      MavenProject project, MojoExecution execution) throws IOException {

    super(workspace, messageSink, conventions.getExecutionStateLocation(project, execution),
        digester.digest());
  }

  @Override
  public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {}

  @Override
  public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
    try {
      commit();
    } catch (IOException e) {
      throw new MojoExecutionException("Could not maintain incremental build state", e);
    }
  }

  @Override
  public void afterExecutionFailure(MojoExecutionEvent event) {}

  @Override
  protected MojoExecutionException newBuildFailureException(int errorCount) {
    return new MojoExecutionException(errorCount
        + " error(s) encountered, see previous message(s) for details");
  }
}
