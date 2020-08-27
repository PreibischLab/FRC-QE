 //by Friedrich Preusser (github.com/Fritze/)
 //August 2020


//For this macro to work it needs the "BigStitcher" update site to be activated.

 //This macro will automatically perform FRC-QE on multiple 3D images.
 //Steps happening here:
 // 1. You select a folder that contains all your images. All files within this folder should be images.
 // 2. Enter your FFT size (i.e. FRC block size). This should be helt constant when comparing images.
 // 3. If you want to also calculate Shannon Entropy, please select the corresponding button.
 // 3. A results folder will be created for each image individually (folder has the name of the image).
 // 4. The macro will perform FRC-QE and Shannon entropy measurements (if selected) for all images and save the results as csv. file in the results folder.


setBatchMode(true);
 

dir = getDirectory("Choose a Directory ");
count = 0;
n = 0;
list = getFileList(dir);
 
number_of_files = list.length;

Dialog.create("FRC-QE macro");
Dialog.addNumber("Enter the FFT size (i.e. FRC block size) you want to use for all images that will be processed.", 200);
Dialog.addCheckbox("Should Shannon entropy be calculated?", true);

entropy = Dialog.getCheckbox();
FFT_size = Dialog.getNumber();

processFiles(dir);


function countFiles(dir) {
	list = getFileList(dir);
 	for (i=0; i<list.length; i++) {
 		if (endsWith(list[i], "/"))
			countFiles(""+dir+list[i]);
        else
 			count++;
      }
  }

function processFiles(dir) {
	for (i=0; i<number_of_files; i++) {
            path = dir+list[i];
            if (!matches(list[i], ".*/.*")) {
            	processFile(path);
            }
    }
	Dialog.create("DONE.");
	Dialog.addMessage("All images were processed.");
	Dialog.show();
 }


function processFile(path) {
	name = File.getName(path);
	dotIndex = indexOf(name, ".");
    name = substring(name, 0, dotIndex);
	 // print the file name
	print("Now processing :", name);
	
	
	
	// open the file
	open(path);
	directory = File.getParent(path)+"/";

	title = getTitle();
	
	// FRC calculation
	selectWindow(title);
	run("Cleared Sample Quality Estimation", "quality_method=[relative FRC (Fourier Ring Correlation)] area_for_quality=[Entire image] fft_size=" + FFT_size + " step_size=1 relative_frc_distance=10");
	close();
	//create results folder
	results_folder_path = directory + "/" + name + "/";
	File.makeDirectory(results_folder_path); 
	// name of the image and csv generated is set here
	path_file = results_folder_path+"FRC_"+name+".csv";
	// Save as csv file
	Table.rename("Image Quality (rFRC)", "Results");
	saveAs("Results", path_file);
	close("Results");
	run("Clear Results");

	if (entropy) {

	
		// Shannon Entropy
		selectWindow(title);
		run("Cleared Sample Quality Estimation", "quality_method=[Shannon Entropy] area_for_quality=[Entire image]");
		close();
		// name of the image and csv generated is set here
		path_file = results_folder_path+"Shannon-entropy_"+name+".csv";
		// Save as csv file
		Table.rename("Image Quality (Shannon Entropy)", "Results");
		saveAs("Results", path_file);
		close("Results");
		run("Clear Results");
		
		// DCT Shannon Entropy
		selectWindow(title);
		run("Cleared Sample Quality Estimation", "quality_method=[Normalized DCT Shannon Entropy] area_for_quality=[Entire image]");
		close();
		// name of the image and csv generated is set here
		path_file = results_folder_path+"DCT-Shannon-entropy_"+name+".csv";
		// Save as csv file
		Table.rename("Image Quality (DCT Shannon Entropy)", "Results");
		saveAs("Results", path_file);
		close("Results");
		run("Clear Results");
		
		// median DCT Shannon Entropy
		selectWindow(title);
		run("Cleared Sample Quality Estimation", "quality_method=[Normalized DCT Shannon Entropy, median filtered] area_for_quality=[Entire image]");
		close();
		// name of the image and csv generated is set here
		path_file = results_folder_path+"median-DCT-Shannon-entropy_"+name+".csv";
		// Save as csv file
		Table.rename("Image Quality (median DCT Shannon Entropy)", "Results");
		saveAs("Results", path_file);
		close("Results");
		run("Clear Results");
		
		// DFT Shannon Entropy
		selectWindow(title);
		run("Cleared Sample Quality Estimation", "quality_method=[Normalized DFT Shannon Entropy] area_for_quality=[Entire image]");
		close();
		// name of the image and csv generated is set here
		path_file = results_folder_path+"DFT-Shannon-entropy_"+name+".csv";
		// Save as csv file
		Table.rename("Image Quality (DFT Shannon Entropy)", "Results");
		saveAs("Results", path_file);
		close("Results");
		run("Clear Results");
		
		close(title);
		print(title + " was processed.");
	}
	print(title + " was processed.");
}


