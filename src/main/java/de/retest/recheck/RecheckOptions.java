package de.retest.recheck;

import static de.retest.recheck.ignore.SearchFilterFiles.getFilterByName;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.retest.recheck.ignore.CompoundFilter;
import de.retest.recheck.ignore.Filter;
import de.retest.recheck.ignore.RecheckIgnoreUtil;
import de.retest.recheck.persistence.ExplicitMutableNamingStrategy;
import de.retest.recheck.persistence.FileNamer;
import de.retest.recheck.persistence.GradleProjectLayout;
import de.retest.recheck.persistence.JunitbasedNamingStrategy;
import de.retest.recheck.persistence.JunitbasedShortNamingStrategy;
import de.retest.recheck.persistence.MavenProjectLayout;
import de.retest.recheck.persistence.NamingStrategy;
import de.retest.recheck.persistence.ProjectLayout;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * This class configures the behaviour of {@link Recheck} and their implementations.
 */
@AllArgsConstructor( access = AccessLevel.PROTECTED )
public class RecheckOptions {

	private final FileNamerStrategy fileNamerStrategy;
	private final NamingStrategy namingStrategy;
	private final ProjectLayout projectLayout;
	private final String suiteName;
	private final boolean reportUploadEnabled;
	private final Filter filter;
	private final boolean addDefaultFilters;

	/**
	 * Creates a shallow copy of the given options. Useful when extending the RecheckOptions to minimize dependencies on
	 * internals.
	 *
	 * Example:
	 *
	 * <pre>
	 * &#64;Override
	 * public RecheckWebOptions build() {
	 * 	return new RecheckWebOptions( super.build(), namingStrategy );
	 * }
	 * </pre>
	 *
	 * or
	 *
	 * <pre>
	 * public RecheckWebOptions( final RecheckOptions superOptions,
	 * 		final AutocheckingCheckNamingStrategy namingStrategy ) {
	 * 	super( superOptions );
	 * 	this.namingStrategy = namingStrategy;
	 * }
	 * </pre>
	 */
	protected RecheckOptions( final RecheckOptions toCopy ) {
		this( toCopy.fileNamerStrategy, toCopy.namingStrategy, toCopy.projectLayout, toCopy.suiteName,
				toCopy.reportUploadEnabled, toCopy.filter, toCopy.addDefaultFilters );
	}

	/**
	 * Factory method for the builder.
	 *
	 * @return A {link RecheckOptionsBuilder}.
	 */
	public static RecheckOptionsBuilder builder() {
		return new RecheckOptionsBuilder();
	}

	/**
	 * Gets the configured {@link FileNamerStrategy} which should be used to identify the tests, locate the golden
	 * masters and report files.
	 *
	 * @return The {@link FileNamerStrategy} to use for locating golden masters and reports.
	 */
	public FileNamerStrategy getFileNamerStrategy() {
		return fileNamerStrategy;
	}

	/**
	 * Gets the suite name which overwrites {@link FileNamerStrategy#getTestClassName()}.
	 *
	 * @return The suite name to use for identifying the golden master.
	 * @implNote If no suite name is provided, the {@link FileNamerStrategy#getTestClassName()} is used.
	 */
	public String getSuiteName() {
		if ( suiteName != null ) {
			return suiteName;
		}
		return namingStrategy.getSuiteName();
	}

	/**
	 * If the report should be uploaded to <a href="https://retest.de/rehub/">rehub</a>.
	 *
	 * @return If the report should be uploaded.
	 * @see Rehub
	 */
	public boolean isReportUploadEnabled() {
		return reportUploadEnabled;
	}

	/**
	 * Gets the configured filter which is used for printing the report after a test.
	 *
	 * @return The filter to use for printing the report.
	 * @implNote If no filter is provided, the default filters are used.
	 */
	public Filter getFilter() {
		if ( !addDefaultFilters ) {
			return filter;
		}
		return new CompoundFilter( Arrays.asList( //
				filter, //
				RecheckIgnoreUtil.loadRecheckIgnore(), //
				RecheckIgnoreUtil.loadRecheckSuiteIgnore( getSuitePath() ) //
		) );
	}

	/**
	 * @return The {@link ProjectLayout} to use (e.g. {@link MavenProjectLayout}, {@link GradleProjectLayout}, ...).
	 */
	public ProjectLayout getProjectLayout() {
		return projectLayout;
	}

	/**
	 * @return The {@link NamingStrategy} to use (e.g. a {@link JunitbasedNamingStrategy}).
	 */
	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	private File getSuitePath() {
		if ( fileNamerStrategy != null ) {
			final FileNamer fileNamer = fileNamerStrategy.createFileNamer( getSuiteName() );
			return fileNamer.getFile( Properties.GOLDEN_MASTER_FILE_EXTENSION );
		}
		return projectLayout.getSuiteFolder( namingStrategy.getSuiteName() ).toFile();
	}

	public static class RecheckOptionsBuilder {

		private FileNamerStrategy fileNamerStrategy;
		private NamingStrategy namingStrategy = new JunitbasedNamingStrategy();
		private ProjectLayout projectLayout = new MavenProjectLayout();
		private String suiteName = null;
		private boolean reportUploadEnabled = false;
		private Filter ignoreFilter = null;
		private final List<Filter> ignoreFilterToAdd = new ArrayList<>();

		protected RecheckOptionsBuilder() {}

		/**
		 * @deprecated Use {@link #namingStrategy} and {@link #projectLayout} instead.
		 *
		 *             Configures the {@link FileNamerStrategy} to identify tests, locate golden masters and report
		 *             files.
		 *
		 * @param fileNamerStrategy
		 *            The strategy to use, defaults to {@link MavenConformFileNamerStrategy}
		 * @return self
		 */
		@Deprecated
		public RecheckOptionsBuilder fileNamerStrategy( final FileNamerStrategy fileNamerStrategy ) {
			this.fileNamerStrategy = fileNamerStrategy;
			return namingStrategy( fileNamerStrategy );
		}

		/**
		 * @param namingStrategy
		 *            The {@link NamingStrategy} that determines how to name tests and suites. Default is
		 *            {@link JunitbasedNamingStrategy}. Other options include {@link JunitbasedShortNamingStrategy} and
		 *            {@link ExplicitMutableNamingStrategy}.
		 * @return self
		 */
		public RecheckOptionsBuilder namingStrategy( final NamingStrategy namingStrategy ) {
			this.namingStrategy = namingStrategy;
			return this;
		}

		/**
		 * @param projectLayout
		 *            The {@link ProjectLayout} that determines where Golden Master and report files are stored and how
		 *            they are named. Default is {@link MavenProjectLayout}.
		 * @return self
		 */
		public RecheckOptionsBuilder projectLayout( final ProjectLayout projectLayout ) {
			this.projectLayout = projectLayout;
			return this;
		}

		/**
		 * Overwrites the suite name from {@link FileNamerStrategy}.
		 *
		 * @param suiteName
		 *            The suite name to identify the golden master. Default:
		 *            {@link FileNamerStrategy#getTestClassName()}.
		 * @return self
		 */
		public RecheckOptionsBuilder suiteName( final String suiteName ) {
			this.suiteName = suiteName;
			return this;
		}

		/**
		 * Enables upload to <a href="https://retest.de/rehub/">rehub</a> so that all reports are stored there. Default:
		 * false.
		 *
		 * @return self
		 */
		public RecheckOptionsBuilder enableReportUpload() {
			reportUploadEnabled = true;
			return this;
		}

		/**
		 * Overwrites the filter used for printing the report after a test. The filter cannot be used in conjunction
		 * with {@link #addIgnore(String)}.
		 *
		 * @param filterName
		 *            The filter to use for printing the differences. The String needs the .filter-extension. Default:
		 *            Loads the ignore files.
		 * @return self
		 */
		public RecheckOptionsBuilder setIgnore( final String filterName ) {
			ignoreFilter = getFilterByName( filterName );
			return this;
		}

		/**
		 * Use to use not ignore anything.
		 *
		 * @return self
		 */
		public RecheckOptionsBuilder ignoreNothing() {
			ignoreFilter = Filter.FILTER_NOTHING;
			return this;
		}

		/**
		 * This creates volatile filters, that make the test pass, but where that state of no differences cannot be
		 * reproduced with neither the review GUI nor the CLI. Therefore it is strongly discouraged that this be used by
		 * customers—it should only be used for internal tests.
		 */
		RecheckOptionsBuilder setIgnore( final Filter filter ) {
			ignoreFilter = filter;
			return this;
		}

		/**
		 * Appends a filter to the default filters. Cannot be used once a filter is overwritten with
		 * {@link #setIgnore(String)}.
		 *
		 * @param filtername
		 *            The filter to add to the ignore. The String needs the .filter-extension.
		 * @return self
		 * @see #setIgnore(String)
		 */
		public RecheckOptionsBuilder addIgnore( final String filtername ) {
			if ( ignoreFilter == null ) {
				ignoreFilterToAdd.add( getFilterByName( filtername ) );
			} else {
				throw new IllegalStateException(
						"Cannot combine `setIgnore(filtername)` and `addIgnore(filtername)`." );
			}
			return this;
		}

		public RecheckOptions build() {
			final Filter filter = ignoreFilter != null ? ignoreFilter : new CompoundFilter( ignoreFilterToAdd );
			final boolean addDefaultFilters = ignoreFilter == null;
			return new RecheckOptions( fileNamerStrategy, namingStrategy, projectLayout, suiteName, reportUploadEnabled,
					filter, addDefaultFilters );
		}
	}
}
