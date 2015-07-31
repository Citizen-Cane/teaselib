package teaselib.core;

import teaselib.TeaseLib;

// TODO Only TeaseLib, since this is called from TeaseScript only

public interface MediaRenderer {
	public void render(TeaseLib teaseLib);

	public interface Threaded {

		/**
		 * Wait for the renderer having completed the introduction phase of its content 
		 */
		public void completeStart();

		/**
		 * Wait for the renderer having completed the mandatory part of its content
		 */
		public void completeMandatory();

		/**
		 * Wait for the renderer to render all its content
		 */
		public void completeAll();

		/**
		 * Instruct the render to end rendering, this might include waiting
		 * until the renderer has finished up rendering its visuals, like
		 * intro/outro sequences
		 */
		public void end();
	}
}
