/**
 * 
 */
package edu.jhu.jacana.qa.feature;

import approxlib.distance.EditDist;
import edu.jhu.jacana.dependency.DependencyTree;
import edu.jhu.jacana.qa.questionanalysis.QuestionWordExtractor;

/**
 * @author Xuchen Yao
 *
 */
@Deprecated
public class QuestionMatchPosFeature extends AbstractFeatureExtractor {

	/**
	 * @param featureName
	 */
	public QuestionMatchPosFeature() {
		super("qword_pos");
	}

	/* (non-Javadoc)
	 * @see edu.jhu.jacana.qa.feature.AbstractFeatureExtractor#extractSingleFeature(approxlib.distance.EditDist, edu.jhu.jacana.dependency.DependencyTree, edu.jhu.jacana.dependency.DependencyTree)
	 */
	@Override
	public String[] extractSingleFeature(EditDist dist, DependencyTree qTree,
			DependencyTree aTree) {
		PosFeature posEx = new PosFeature();
		String[] features = posEx.extractSingleFeature(dist, qTree, aTree);
		String q = QuestionWordExtractor.getQuestionWords(qTree);
		for (int i=0; i<features.length; i++) {
			features[i] = q + "|" + features[i];
		}
		return features;
	}

}
