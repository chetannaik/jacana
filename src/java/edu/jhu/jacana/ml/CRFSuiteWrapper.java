/** 
 * Copyright 2011-2012
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For a complete copy of the license please see the file LICENSE distributed 
 * with the cleartk-syntax-berkeley project or visit 
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */

package edu.jhu.jacana.ml;

import static edu.jhu.jacana.util.PlatformDetection.ARCH_X86_32;
import static edu.jhu.jacana.util.PlatformDetection.ARCH_X86_64;
import static edu.jhu.jacana.util.PlatformDetection.OS_WINDOWS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.jhu.jacana.util.InputStreamHandler;
import edu.jhu.jacana.util.PlatformDetection;
import edu.jhu.jacana.util.StringUtils;

import de.tudarmstadt.ukp.dkpro.core.api.resources.ResourceUtils;

/**
 * <br>
 * Copyright (c) 2011-2012, Technische Universität Darmstadt <br>
 * All rights reserved.
 * 
 * @author Martin Riedl
 * 
 * @author Xuchen Yao
 * @version 2012-11-5
 * took out most UIMA dependencies. 
 */

public class CRFSuiteWrapper {
	static Logger logger = Logger.getLogger(CRFSuiteWrapper.class.getName());

	private File executable;
	
	// save the last trained model to this location
	private File modelFile;
	
	protected String defaultTrainParam = "-p c1=0.0 -p c2=1.0";
	
/*
#    -t, --test          Report the performance of the model on the data
#    -r, --reference     Output the reference labels in the input data
#    -p, --probability   Output the probability of the label sequences
#    -i, --marginal      Output the marginal probabilities of items
*/
	protected String defaultTestParam = "-tpri";
	
	public void setTrainParam(String param) { defaultTrainParam = param;}
	public void setTestParam(String param) { defaultTestParam = param;}

	public CRFSuiteWrapper(String modelFile) {
		this();
		logger.info("Loading CRF model from " + modelFile);
		this.modelFile = new File(modelFile);
	}
	
	public CRFSuiteWrapper() {
        //ConsoleHandler ch = new ConsoleHandler();
        //ch.setLevel(Level.FINE);
        //logger.addHandler(ch);
		//logger.setLevel(Level.FINE);
		Executables exec = new Executables();
		if (exec.isInstalled()) {
			logger.fine("The CRFSuite is installed on the system");
			executable = new File(exec.getExecutableName());
		} else {

			this.executable = exec.getExecutable();
			if (!exec.isInstalled(this.executable.getAbsolutePath())) {
				logger.warning("The CRFSuite binary is not available for the current operation system, please install it!");
			} else {
				logger.fine("The CRFSuite binary is successfully extracted");
			}
		}
	}

	class Executables {
		PlatformDetection pd = new PlatformDetection();

		public Executables() {

			// windows 64 should also be able to execute win32 files --> change
			// it to win 32, this can be fixed, when a win 64 Bit executable is
			// available
			if (pd.getOs().equals(OS_WINDOWS) && pd.getArch().equals(ARCH_X86_64)) {
				pd.setArch(ARCH_X86_32);
			}
		}

		public String getExecutablePath() {

			String[] path = new String[] { "crfsuite", pd.toString(), "bin" };
			// don't use File.separater here otherwise on Windows it won't find the path
			String sep = "/";
			String p = "";
			for (String s : path) {
				p += s + sep;
			}
			return p;

		}

		public boolean isInstalled() {
			return isInstalled("crfsuite");
		}

		public boolean isInstalled(String path) {
			ProcessBuilder builder = new ProcessBuilder(path, "-h");
			builder.redirectErrorStream();
			Process p;

			try {
				p = builder.start();
				InputStreamHandler<StringBuffer> output = InputStreamHandler.getInputStreamAsBufferedString(p.getInputStream());
				try {
					p.waitFor();
					output.join();
				} catch (InterruptedException e) {
					logger.warning(e.getMessage());
				}
				StringBuffer buffer = output.getBuffer();
				if (buffer.length() < 8) {
					logger.warning("CRFSuite could not be executed!");
				}
				return buffer.length() >= 8 && buffer.substring(0, 8).equals("CRFSuite");
			} catch (IOException e) {
				// Path is not available
				logger.fine(e.getMessage());
			}
			return false;
		}

		public String getExecutableName() {
			return "crfsuite" + pd.getExecutableSuffix();
		}

		public File getExecutable() {
			String loc = getExecutablePath() + getExecutableName();
			URL crfExecUrl = getClass().getResource(loc);
			crfExecUrl = ClassLoader.getSystemResource(loc);
			logger.fine("CrfSuite Location " + loc);
			logger.fine("CrfSuite Url: " + crfExecUrl);
			File f;
			try {
				if (crfExecUrl != null) {
					f = new File(ResourceUtils.getUrlAsFile(crfExecUrl, true).toURI().getPath());
					if (!f.exists()) {
						f = new File(URLDecoder.decode(f.getAbsolutePath(), ("UTF-8")));
					}
					f.setExecutable(true);
					return f;
				}
				logger.warning("The executable could not be found at " + loc);
				return null;
			} catch (IOException e) {
				e.printStackTrace();

				return null;
			}

		}

	}
	
	public void trainClassifier(String featureLines)
			throws IOException {
		File featureFile = File.createTempFile("features-train", ".crfsuite");
		featureFile.deleteOnExit();
		logger.fine("Write features to train to " + featureFile.getAbsolutePath());
		BufferedWriter out = new BufferedWriter(new FileWriter(featureFile));
		out.write(featureLines);
		out.close();
		modelFile = File.createTempFile("crfsuite", ".model");
		modelFile.deleteOnExit();
		trainClassifier(modelFile.getAbsolutePath(), featureFile.getAbsolutePath());
	}

	public void trainClassifier(String model, String trainingDataFile)
			throws IOException {

		StringBuffer cmd = new StringBuffer();
		cmd.append(executable.getPath());
		cmd.append(" ");
		cmd.append("learn");
		cmd.append(" ");
		cmd.append("-m");
		cmd.append(" ");
		cmd.append(model);
		cmd.append(" ");
		cmd.append(this.defaultTrainParam);
		cmd.append(" ");
		cmd.append(trainingDataFile);
		Process p = Runtime.getRuntime().exec(cmd.toString());

		InputStream stdIn = p.getInputStream();
		InputStreamHandler<List<String>> ishIn = InputStreamHandler.getInputStreamAsList(stdIn);

		InputStream stdErr = p.getErrorStream();
		InputStreamHandler<StringBuffer> ishErr = InputStreamHandler.getInputStreamAsBufferedString(stdErr);

		try {
			p.waitFor();
			ishIn.join();
			ishErr.join();
		} catch (InterruptedException e) {
			logger.warning(e.getMessage());
		}

		String error = ishErr.getBuffer().toString();
		if (error.length() > 0)
			logger.warning(error.replaceAll("(^\\[)|([,])|(]$)", "\n"));
			//logger.warning(error);
		logger.info(ishIn.getBuffer().toString().replaceAll("(^\\[)|([,])|(]$)", "\n"));
		//logger.info(ishIn.getBuffer().toString());
		stdErr.close();
		stdIn.close();

	}

	public List<String> classifyFeatures(String featureFile, String modelFile)
			throws IOException {
		return classifyFeatures(new File(featureFile), new File(modelFile));
	}

	public List<String> classifyFeatures(String featureLines) 
			throws IOException {
		return classifyFeatures(featureLines, modelFile) ;
	}

	public List<String> classifyFeatures(String featureLines, File modelFile) 
			throws IOException {

		File featureFile = File.createTempFile("features", ".crfsuite");
		featureFile.deleteOnExit();
		logger.fine("Write features to classify to " + featureFile.getAbsolutePath());
		BufferedWriter out = new BufferedWriter(new FileWriter(featureFile));
		out.write(featureLines);
		out.close();
		return classifyFeatures(featureFile, modelFile);
	}

	private List<String> classifyFeatures(File featureFile, File modelFile) throws IOException {
		List<String> posTags = new ArrayList<String>();
		StringBuffer cmd = new StringBuffer();

		cmd.append(executable.getPath());
		cmd.append(" tag -m ");
		cmd.append(modelFile.getAbsolutePath());
		cmd.append(" ");
		cmd.append(this.defaultTestParam);
		cmd.append(" ");
		cmd.append(featureFile.getAbsolutePath());
		cmd.append(" ");
		//System.out.println(cmd.toString());
		Process p = Runtime.getRuntime().exec(cmd.toString());

		InputStream stdIn = p.getInputStream();
		InputStreamHandler<List<String>> ishIn = InputStreamHandler.getInputStreamAsList(stdIn);

		InputStream stdErr = p.getErrorStream();
		InputStreamHandler<StringBuffer> ishErr = InputStreamHandler.getInputStreamAsBufferedString(stdErr);

		try {
			p.waitFor();
			ishIn.join();
			ishErr.join();
		} catch (InterruptedException e) {
			logger.warning(e.getMessage());
		}
		String error = ishErr.getBuffer().toString();
		if (error.length() > 0)
			logger.warning(ishErr.getBuffer().toString().replaceAll("(^\\[)|([,])|(]$)", "\n"));
		posTags = ishIn.getBuffer();
		
		if (posTags.size() > 0) {
			if (posTags.get(posTags.size() - 1).trim().length() == 0) {
			// if it's an empty line
				posTags.remove(posTags.size() - 1);
			}
		}
		stdErr.close();
		stdIn.close();
		return posTags;
	}

	public static void main(String[] argv) throws IOException {
		
		String[] features = new String[] {
    "O Word_Three LCWord_three CapitalType_INITIAL_UPPERCASE L0OOB1 L1OOB2 R0_sequence R0_TypePath_Pos_NN R0_TypePath_Stem_sequenc R1_elements R1_TypePath_Pos_NNS R1_TypePath_Stem_element TypePath_Pos_CD TypePath_Stem_Three PrevNEMTokenLabel_L0OOB1 PrevNEMTokenLabel_L1OOB2",
    "O Word_sequence LCWord_sequence CapitalType_ALL_LOWERCASE Prefix3_seq Suffix3_nce Suffix4_ence Suffix5_uence L0_Three L0_TypePath_Pos_CD L0_TypePath_Stem_Three L1OOB1 R0_elements R0_TypePath_Pos_NNS R0_TypePath_Stem_element R1_are R1_TypePath_Pos_VBP R1_TypePath_Stem_are TypePath_Pos_NN TypePath_Stem_sequenc PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1OOB1",
    "O Word_elements LCWord_elements CapitalType_ALL_LOWERCASE Prefix3_ele Suffix3_nts Suffix4_ents Suffix5_ments L0_sequence L0_TypePath_Pos_NN L0_TypePath_Stem_sequenc L1_Three L1_TypePath_Pos_CD L1_TypePath_Stem_Three R0_are R0_TypePath_Pos_VBP R0_TypePath_Stem_are R1_shown R1_TypePath_Pos_VBN R1_TypePath_Stem_shown TypePath_Pos_NNS TypePath_Stem_element PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_are LCWord_are CapitalType_ALL_LOWERCASE L0_elements L0_TypePath_Pos_NNS L0_TypePath_Stem_element L1_sequence L1_TypePath_Pos_NN L1_TypePath_Stem_sequenc R0_shown R0_TypePath_Pos_VBN R0_TypePath_Stem_shown R1_to R1_TypePath_Pos_TO R1_TypePath_Stem_to TypePath_Pos_VBP TypePath_Stem_are PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_shown LCWord_shown CapitalType_ALL_LOWERCASE L0_are L0_TypePath_Pos_VBP L0_TypePath_Stem_are L1_elements L1_TypePath_Pos_NNS L1_TypePath_Stem_element R0_to R0_TypePath_Pos_TO R0_TypePath_Stem_to R1_be R1_TypePath_Pos_VB R1_TypePath_Stem_be TypePath_Pos_VBN TypePath_Stem_shown PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_to LCWord_to CapitalType_ALL_LOWERCASE L0_shown L0_TypePath_Pos_VBN L0_TypePath_Stem_shown L1_are L1_TypePath_Pos_VBP L1_TypePath_Stem_are R0_be R0_TypePath_Pos_VB R0_TypePath_Stem_be R1_required R1_TypePath_Pos_VBN R1_TypePath_Stem_requir TypePath_Pos_TO TypePath_Stem_to PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_be LCWord_be CapitalType_ALL_LOWERCASE L0_to L0_TypePath_Pos_TO L0_TypePath_Stem_to L1_shown L1_TypePath_Pos_VBN L1_TypePath_Stem_shown R0_required R0_TypePath_Pos_VBN R0_TypePath_Stem_requir R1_for R1_TypePath_Pos_IN R1_TypePath_Stem_for TypePath_Pos_VB TypePath_Stem_be PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_required LCWord_required CapitalType_ALL_LOWERCASE Prefix3_req Suffix3_red Suffix4_ired Suffix5_uired L0_be L0_TypePath_Pos_VB L0_TypePath_Stem_be L1_to L1_TypePath_Pos_TO L1_TypePath_Stem_to R0_for R0_TypePath_Pos_IN R0_TypePath_Stem_for R1_accurate R1_TypePath_Pos_JJ R1_TypePath_Stem_accur TypePath_Pos_VBN TypePath_Stem_requir PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_for LCWord_for CapitalType_ALL_LOWERCASE L0_required L0_TypePath_Pos_VBN L0_TypePath_Stem_requir L1_be L1_TypePath_Pos_VB L1_TypePath_Stem_be R0_accurate R0_TypePath_Pos_JJ R0_TypePath_Stem_accur R1_and R1_TypePath_Pos_CC R1_TypePath_Stem_and TypePath_Pos_IN TypePath_Stem_for PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_accurate LCWord_accurate CapitalType_ALL_LOWERCASE Prefix3_acc Suffix3_ate Suffix4_rate Suffix5_urate L0_for L0_TypePath_Pos_IN L0_TypePath_Stem_for L1_required L1_TypePath_Pos_VBN L1_TypePath_Stem_requir R0_and R0_TypePath_Pos_CC R0_TypePath_Stem_and R1_efficient R1_TypePath_Pos_JJ R1_TypePath_Stem_effici TypePath_Pos_JJ TypePath_Stem_accur PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_and LCWord_and CapitalType_ALL_LOWERCASE L0_accurate L0_TypePath_Pos_JJ L0_TypePath_Stem_accur L1_for L1_TypePath_Pos_IN L1_TypePath_Stem_for R0_efficient R0_TypePath_Pos_JJ R0_TypePath_Stem_effici R1_transcription R1_TypePath_Pos_NN R1_TypePath_Stem_transcript TypePath_Pos_CC TypePath_Stem_and PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_efficient LCWord_efficient CapitalType_ALL_LOWERCASE Prefix3_eff Suffix3_ent Suffix4_ient Suffix5_cient L0_and L0_TypePath_Pos_CC L0_TypePath_Stem_and L1_accurate L1_TypePath_Pos_JJ L1_TypePath_Stem_accur R0_transcription R0_TypePath_Pos_NN R0_TypePath_Stem_transcript R1_termination R1_TypePath_Pos_NN R1_TypePath_Stem_termin TypePath_Pos_JJ TypePath_Stem_effici PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_transcription LCWord_transcription CapitalType_ALL_LOWERCASE Prefix3_tra Suffix3_ion Suffix4_tion Suffix5_ption L0_efficient L0_TypePath_Pos_JJ L0_TypePath_Stem_effici L1_and L1_TypePath_Pos_CC L1_TypePath_Stem_and R0_termination R0_TypePath_Pos_NN R0_TypePath_Stem_termin R1_by R1_TypePath_Pos_IN R1_TypePath_Stem_by TypePath_Pos_NN TypePath_Stem_transcript PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_termination LCWord_termination CapitalType_ALL_LOWERCASE Prefix3_ter Suffix3_ion Suffix4_tion Suffix5_ation L0_transcription L0_TypePath_Pos_NN L0_TypePath_Stem_transcript L1_efficient L1_TypePath_Pos_JJ L1_TypePath_Stem_effici R0_by R0_TypePath_Pos_IN R0_TypePath_Stem_by R1_RNA R1_TypePath_Pos_NNP R1_TypePath_Stem_RNA TypePath_Pos_NN TypePath_Stem_termin PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_by LCWord_by CapitalType_ALL_LOWERCASE L0_termination L0_TypePath_Pos_NN L0_TypePath_Stem_termin L1_transcription L1_TypePath_Pos_NN L1_TypePath_Stem_transcript R0_RNA R0_TypePath_Pos_NNP R0_TypePath_Stem_RNA R1_polymerase R1_TypePath_Pos_NN R1_TypePath_Stem_polymeras TypePath_Pos_IN TypePath_Stem_by PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "B-GENE Word_RNA LCWord_rna CapitalType_ALL_UPPERCASE L0_by L0_TypePath_Pos_IN L0_TypePath_Stem_by L1_termination L1_TypePath_Pos_NN L1_TypePath_Stem_termin R0_polymerase R0_TypePath_Pos_NN R0_TypePath_Stem_polymeras R1_I R1_TypePath_Pos_PRP R1_TypePath_Stem_I TypePath_Pos_NNP TypePath_Stem_RNA PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "I-GENE Word_polymerase LCWord_polymerase CapitalType_ALL_LOWERCASE Prefix3_pol Suffix3_ase Suffix4_rase Suffix5_erase L0_RNA L0_TypePath_Pos_NNP L0_TypePath_Stem_RNA L1_by L1_TypePath_Pos_IN L1_TypePath_Stem_by R0_I R0_TypePath_Pos_PRP R0_TypePath_Stem_I R1_( R1_TypePath_Pos_-LRB- R1_TypePath_Stem_( TypePath_Pos_NN TypePath_Stem_polymeras PrevNEMTokenLabel_L0_B-GENE PrevNEMTokenLabel_L1_O Gazetteer_entrez_genes.txt Gazetteer_entrez_genes.txt",
    "I-GENE Word_I LCWord_i CapitalType_ALL_UPPERCASE NumericType_ROMAN_NUMERAL L0_polymerase L0_TypePath_Pos_NN L0_TypePath_Stem_polymeras L1_RNA L1_TypePath_Pos_NNP L1_TypePath_Stem_RNA R0_( R0_TypePath_Pos_-LRB- R0_TypePath_Stem_( R1_pol R1_TypePath_Pos_NN R1_TypePath_Stem_pol TypePath_Pos_PRP TypePath_Stem_I PrevNEMTokenLabel_L0_I-GENE PrevNEMTokenLabel_L1_B-GENE",
    "O Word_( LCWord_( L0_I L0_TypePath_Pos_PRP L0_TypePath_Stem_I L1_polymerase L1_TypePath_Pos_NN L1_TypePath_Stem_polymeras R0_pol R0_TypePath_Pos_NN R0_TypePath_Stem_pol R1_I R1_TypePath_Pos_PRP R1_TypePath_Stem_I TypePath_Pos_-LRB- TypePath_Stem_( PrevNEMTokenLabel_L0_I-GENE PrevNEMTokenLabel_L1_I-GENE",
    "B-GENE Word_pol LCWord_pol CapitalType_ALL_LOWERCASE L0_( L0_TypePath_Pos_-LRB- L0_TypePath_Stem_( L1_I L1_TypePath_Pos_PRP L1_TypePath_Stem_I R0_I R0_TypePath_Pos_PRP R0_TypePath_Stem_I R1_) R1_TypePath_Pos_-RRB- R1_TypePath_Stem_) TypePath_Pos_NN TypePath_Stem_pol PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_I-GENE Gazetteer_entrez_genes.txt",
    "I-GENE Word_I LCWord_i CapitalType_ALL_UPPERCASE NumericType_ROMAN_NUMERAL L0_pol L0_TypePath_Pos_NN L0_TypePath_Stem_pol L1_( L1_TypePath_Pos_-LRB- L1_TypePath_Stem_( R0_) R0_TypePath_Pos_-RRB- R0_TypePath_Stem_) R1_assayed R1_TypePath_Pos_VBD R1_TypePath_Stem_assay TypePath_Pos_PRP TypePath_Stem_I PrevNEMTokenLabel_L0_B-GENE PrevNEMTokenLabel_L1_O",
    "O Word_) LCWord_) L0_I L0_TypePath_Pos_PRP L0_TypePath_Stem_I L1_pol L1_TypePath_Pos_NN L1_TypePath_Stem_pol R0_assayed R0_TypePath_Pos_VBD R0_TypePath_Stem_assay R1_both R1_TypePath_Pos_DT R1_TypePath_Stem_both TypePath_Pos_-RRB- TypePath_Stem_) PrevNEMTokenLabel_L0_I-GENE PrevNEMTokenLabel_L1_B-GENE",
    "O Word_assayed LCWord_assayed CapitalType_ALL_LOWERCASE Prefix3_ass Suffix3_yed Suffix4_ayed L0_) L0_TypePath_Pos_-RRB- L0_TypePath_Stem_) L1_I L1_TypePath_Pos_PRP L1_TypePath_Stem_I R0_both R0_TypePath_Pos_DT R0_TypePath_Stem_both R1_in R1_TypePath_Pos_IN R1_TypePath_Stem_in TypePath_Pos_VBD TypePath_Stem_assay PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_I-GENE",
    "O Word_both LCWord_both CapitalType_ALL_LOWERCASE L0_assayed L0_TypePath_Pos_VBD L0_TypePath_Stem_assay L1_) L1_TypePath_Pos_-RRB- L1_TypePath_Stem_) R0_in R0_TypePath_Pos_IN R0_TypePath_Stem_in R1_a R1_TypePath_Pos_DT R1_TypePath_Stem_a TypePath_Pos_DT TypePath_Stem_both PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_in LCWord_in CapitalType_ALL_LOWERCASE L0_both L0_TypePath_Pos_DT L0_TypePath_Stem_both L1_assayed L1_TypePath_Pos_VBD L1_TypePath_Stem_assay R0_a R0_TypePath_Pos_DT R0_TypePath_Stem_a R1_cell-free R1_TypePath_Pos_JJ R1_TypePath_Stem_cell-fre TypePath_Pos_IN TypePath_Stem_in PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_a LCWord_a CapitalType_ALL_LOWERCASE L0_in L0_TypePath_Pos_IN L0_TypePath_Stem_in L1_both L1_TypePath_Pos_DT L1_TypePath_Stem_both R0_cell-free R0_TypePath_Pos_JJ R0_TypePath_Stem_cell-fre R1_transcription R1_TypePath_Pos_NN R1_TypePath_Stem_transcript TypePath_Pos_DT TypePath_Stem_a PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_cell-free LCWord_cell-free CapitalType_ALL_LOWERCASE ContainsHyphen_CONTAINS_HYPHEN Prefix3_cel Suffix3_ree Suffix4_free Suffix5_-free L0_a L0_TypePath_Pos_DT L0_TypePath_Stem_a L1_in L1_TypePath_Pos_IN L1_TypePath_Stem_in R0_transcription R0_TypePath_Pos_NN R0_TypePath_Stem_transcript R1_system R1_TypePath_Pos_NN R1_TypePath_Stem_system TypePath_Pos_JJ TypePath_Stem_cell-fre PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_transcription LCWord_transcription CapitalType_ALL_LOWERCASE Prefix3_tra Suffix3_ion Suffix4_tion Suffix5_ption L0_cell-free L0_TypePath_Pos_JJ L0_TypePath_Stem_cell-fre L1_a L1_TypePath_Pos_DT L1_TypePath_Stem_a R0_system R0_TypePath_Pos_NN R0_TypePath_Stem_system R1_and R1_TypePath_Pos_CC R1_TypePath_Stem_and TypePath_Pos_NN TypePath_Stem_transcript PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_system LCWord_system CapitalType_ALL_LOWERCASE Prefix3_sys Suffix3_tem L0_transcription L0_TypePath_Pos_NN L0_TypePath_Stem_transcript L1_cell-free L1_TypePath_Pos_JJ L1_TypePath_Stem_cell-fre R0_and R0_TypePath_Pos_CC R0_TypePath_Stem_and R1_in R1_TypePath_Pos_IN R1_TypePath_Stem_in TypePath_Pos_NN TypePath_Stem_system PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_and LCWord_and CapitalType_ALL_LOWERCASE L0_system L0_TypePath_Pos_NN L0_TypePath_Stem_system L1_transcription L1_TypePath_Pos_NN L1_TypePath_Stem_transcript R0_in R0_TypePath_Pos_IN R0_TypePath_Stem_in R1_vivo R1_TypePath_Pos_RB R1_TypePath_Stem_vivo TypePath_Pos_CC TypePath_Stem_and PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_in LCWord_in CapitalType_ALL_LOWERCASE L0_and L0_TypePath_Pos_CC L0_TypePath_Stem_and L1_system L1_TypePath_Pos_NN L1_TypePath_Stem_system R0_vivo R0_TypePath_Pos_RB R0_TypePath_Stem_vivo R1_after R1_TypePath_Pos_IN R1_TypePath_Stem_after TypePath_Pos_IN TypePath_Stem_in PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_vivo LCWord_vivo CapitalType_ALL_LOWERCASE L0_in L0_TypePath_Pos_IN L0_TypePath_Stem_in L1_and L1_TypePath_Pos_CC L1_TypePath_Stem_and R0_after R0_TypePath_Pos_IN R0_TypePath_Stem_after R1_transfection R1_TypePath_Pos_NN R1_TypePath_Stem_transfect TypePath_Pos_RB TypePath_Stem_vivo PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_after LCWord_after CapitalType_ALL_LOWERCASE L0_vivo L0_TypePath_Pos_RB L0_TypePath_Stem_vivo L1_in L1_TypePath_Pos_IN L1_TypePath_Stem_in R0_transfection R0_TypePath_Pos_NN R0_TypePath_Stem_transfect R1_of R1_TypePath_Pos_IN R1_TypePath_Stem_of TypePath_Pos_IN TypePath_Stem_after PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_transfection LCWord_transfection CapitalType_ALL_LOWERCASE Prefix3_tra Suffix3_ion Suffix4_tion Suffix5_ction L0_after L0_TypePath_Pos_IN L0_TypePath_Stem_after L1_vivo L1_TypePath_Pos_RB L1_TypePath_Stem_vivo R0_of R0_TypePath_Pos_IN R0_TypePath_Stem_of R1_rDNA R1_TypePath_Pos_NNP R1_TypePath_Stem_rDNA TypePath_Pos_NN TypePath_Stem_transfect PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_of LCWord_of CapitalType_ALL_LOWERCASE L0_transfection L0_TypePath_Pos_NN L0_TypePath_Stem_transfect L1_after L1_TypePath_Pos_IN L1_TypePath_Stem_after R0_rDNA R0_TypePath_Pos_NNP R0_TypePath_Stem_rDNA R1_minigene R1_TypePath_Pos_NN R1_TypePath_Stem_minigen TypePath_Pos_IN TypePath_Stem_of PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_rDNA LCWord_rdna CapitalType_MIXED_CASE L0_of L0_TypePath_Pos_IN L0_TypePath_Stem_of L1_transfection L1_TypePath_Pos_NN L1_TypePath_Stem_transfect R0_minigene R0_TypePath_Pos_NN R0_TypePath_Stem_minigen R1_constructs R1_TypePath_Pos_NNS R1_TypePath_Stem_construct TypePath_Pos_NNP TypePath_Stem_rDNA PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_minigene LCWord_minigene CapitalType_ALL_LOWERCASE Prefix3_min Suffix3_ene Suffix4_gene Suffix5_igene L0_rDNA L0_TypePath_Pos_NNP L0_TypePath_Stem_rDNA L1_of L1_TypePath_Pos_IN L1_TypePath_Stem_of R0_constructs R0_TypePath_Pos_NNS R0_TypePath_Stem_construct R1_into R1_TypePath_Pos_IN R1_TypePath_Stem_into TypePath_Pos_NN TypePath_Stem_minigen PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_constructs LCWord_constructs CapitalType_ALL_LOWERCASE Prefix3_con Suffix3_cts Suffix4_ucts Suffix5_ructs L0_minigene L0_TypePath_Pos_NN L0_TypePath_Stem_minigen L1_rDNA L1_TypePath_Pos_NNP L1_TypePath_Stem_rDNA R0_into R0_TypePath_Pos_IN R0_TypePath_Stem_into R1_3T6 R1_TypePath_Pos_CD R1_TypePath_Stem_3T6 TypePath_Pos_NNS TypePath_Stem_construct PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_into LCWord_into CapitalType_ALL_LOWERCASE L0_constructs L0_TypePath_Pos_NNS L0_TypePath_Stem_construct L1_minigene L1_TypePath_Pos_NN L1_TypePath_Stem_minigen R0_3T6 R0_TypePath_Pos_CD R0_TypePath_Stem_3T6 R1_cells R1_TypePath_Pos_NNS R1_TypePath_Stem_cell TypePath_Pos_IN TypePath_Stem_into PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_3T6 LCWord_3t6 CapitalType_ALL_UPPERCASE NumericType_ALPHANUMERIC L0_into L0_TypePath_Pos_IN L0_TypePath_Stem_into L1_constructs L1_TypePath_Pos_NNS L1_TypePath_Stem_construct R0_cells R0_TypePath_Pos_NNS R0_TypePath_Stem_cell R1_. R1_TypePath_Pos_. R1_TypePath_Stem_. TypePath_Pos_CD TypePath_Stem_3T6 PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_cells LCWord_cells CapitalType_ALL_LOWERCASE L0_3T6 L0_TypePath_Pos_CD L0_TypePath_Stem_3T6 L1_into L1_TypePath_Pos_IN L1_TypePath_Stem_into R0_. R0_TypePath_Pos_. R0_TypePath_Stem_. R1OOB1 TypePath_Pos_NNS TypePath_Stem_cell PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
    "O Word_. LCWord_. L0_cells L0_TypePath_Pos_NNS L0_TypePath_Stem_cell L1_3T6 L1_TypePath_Pos_CD L1_TypePath_Stem_3T6 R0OOB1 R1OOB2 TypePath_Pos_. TypePath_Stem_. PrevNEMTokenLabel_L0_O PrevNEMTokenLabel_L1_O",
		};
		StringBuffer sb = new StringBuffer();
		for (String f:features) {
			sb.append(StringUtils.joinWithTabs(f.split("\\s+")));
			sb.append("\n");
		}
		CRFSuiteWrapper crf = new CRFSuiteWrapper();
		// make sure that each training instance is terminated by a new line!
		String featureLines = sb.toString();
		crf.trainClassifier(featureLines);
		List<String> labels = crf.classifyFeatures(featureLines);
		System.out.println(labels);
	}
}
