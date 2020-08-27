
<div align="center">

# FRC-QE documentation

</div>

### Content

* _**1.	Introduction & Overview**_
* _**2.	Download**_
* _**3.	Running FRC-QE**_
* _**4. Output**_
* _**5.	Automating FRC-QE**_
* _**6.	Citations**_
<br />
<br />

<div style="text-align: justify">
 
### 1.	Introduction & Overview

The Fourier ring correlation quality estimate (FRC-QE) is a measure for robustly estimating depth-dependent differences in image quality within three-dimensional image stacks. Specifically, this tool was designed to provide users with a robust metric when assessing efficiency of different optical clearing methods in brain organoids. However, the tool can be used on any kind fluorescent microscopy image, i.e. where users want to compare image quality across the whole stack.

We base our algorithm on previous work regarding the use of Fourier ring correlation in electron _(1,2)_ and fluorescent microscopy _(3-5)_. Generally, correlating frequencies of two images describing the same object can be used to estimate the resolution of these images. We take advantage of the fact that consecutive image planes along the z-axis are very similar due to the axial extent of the PSF. Hence, integrating the correlation between two z-slices over all frequencies gives a robust quality metric, with low frequency correlation indicating low image quality.

<br />
<br />

### 2.	Download

The FRC-QE plugin is part of the BigStitcher update site that can be downloaded via the Fiji Updater. Go to ```Help > Update …```, click ```Manage update sites``` and select BigStitcher in the list. The click ```“Apply changes”``` and restart Fiji. You will now find the FRC-QE plugin under ```Plugins```.

The source code is available on [GitHub](https://github.com/PreibischLab/quality-estimation). If you encounter bugs or want to report a feature request, please report everything there.

<br />
<br />

### 3.	Running FRC-QE

For FRC-QE to run properly, you first need to open a three-dimensional image stack in Fiji.
If you want to run the algorithm only on a small part of your image, simply draw a rectangular ROI around the region you want to quantify. This can be helpful when the image data is particularly large or contains imaging artifacts you want to exclude from the analysis.


Next, go to ```Plugins > FRC-QE```. The following window will show up:

![First pop-up](https://github.com/PreibischLab/FRC-QE/blob/master/screenshots/image_1.png)

After selecting the image you want to process, you can also chose the method you want to use to assess image quality over your 3D stack. Besides FRC-QE, you can also compare this measure against variations of the images Shannon entropy, an algorithm that has been previously used for image compression and autofocus _(6)_. 

When choosing FRC-QE as quality measure, the next window will look like this:

 
![Second pop-up](https://github.com/PreibischLab/FRC-QE/blob/master/screenshots/image_2.png)

First, you have to decide on which part of the 3D stack you want to run FRC-QE. You have three options:

* _**(a)**_	If you want integrate over the whole image select “Entire image”. 
* _**(b)**_	If you want to only look at a small part of the image please select _“Use selected 2D ROI”_ and the algorithm will only run on the previously selected ROI.
* _**(c)**_	If you want to exclude certain z-slices within the stack, you can also draw a 3D ROI (_“Define interactively (3D with BDV)”_).

<br />

#### _Parameters that have to be defined:_

* _**FFT size (xy):**_

To better average the FRC within one image plane, we implemented FRC-QE in a block-wise manner, where individual blocks of adjacent planes are compared. By splitting the image into smaller blocks, we diminish the influence of small imaging artifacts on the final result. After calculating the FRC-QE for the individual blocks, we take the median of all blocks within one image as final quality score. The FFT size defines the size of these blocks in pixels. E.g. if you have a 400x400 px image, setting the FFT size to 200, will lead to 4 equally sized blocks of 200x200 px each. Smaller blocks will give more localized quality information, but are also more susceptible to noise.

* _**Step size (z):**_

The step size defines the number of slices that will be used for the analysis. Default is 1, meaning that all slices will be used. If you put the value to 10, only every 10th slice will be correlated to its adjacent slices.

* _**Relative FRC distance:**_

This number defines the relative positioning of the control slice that will be used to estimate unspecific high-frequency noise such as camera noise. This noise will be subtracted from the final result in each slice. This number should be large enough so that slices do not share biological meaningful structures so that everything that correlates at high frequencies in Fourier space can be assumed to be unspecific noise. Hence, this number will depend on you imaging settings, your PSF and the type of staining. Default is 10.

Visualize result as image:
If activated, the plugin will additionally create a new image that displays the individual blocks colored by a grey value that corresponds to their respective FRC-QE score. You can overlay this image with the original input image for visual control of the result. 

<br />
<br />

### 4. Output

Once FRC-QE has been calculated for your image stack, two new windows will pop up:

* _**A.	Image Quality Plot**_
![Third pop-up](https://github.com/PreibischLab/FRC-QE/blob/master/screenshots/image_3.png)
This gives you an overview over the image quality across your 3D stack. The x-axis shows the z-position within the stack and the y-axis the corresponding value at that position. You can save this image under ```File > Save```.

* _**B.	Image Quality table**_
![Fourth](https://github.com/PreibischLab/FRC-QE/blob/master/screenshots/image_4.png)
The second window is a table with all raw values that have been calculated, one value per slice. You can save these values under ```File > Save As```.

<br />
<br />

### 5.	Automating FRC-QE

The FRC-QE is fully scriptable, so that you can run it on multiple images at once, e.g. to compare protocols or imaging parameters. An example macro for automated analysis of multiple images can be found [here](https://github.com/PreibischLab/FRC-QE/blob/master/FRC-QE_automated_macro.ijm). Example notebooks in R for processing the data are available [here](https://github.com/PreibischLab/FRC-QE/tree/master/analysis_scripts).

<br />
<br />

### 6.	Citations

If you find this plugin useful please support us by cite our paper:
<br />

XXX

<br />

#### Background:

* *(1) Heel, Marin Van. “Similarity Measures between Images.” Ultramicroscopy 21, no. 1 (1987): 95–100. https://doi.org/10.1016/0304-3991(87)90010-6.*

* *(2) Saxton, W. O., and W. Baumeister. “The Correlation Averaging of a Regularly Arranged Bacterial Cell Envelope Protein.” Journal of Microscopy 127, no. 2 (August 1982): 127–38. https://doi.org/10.1111/j.1365-2818.1982.tb00405.x.*


* *(3) Banterle, Niccolò, Khanh Huy Bui, Edward A. Lemke, and Martin Beck. “Fourier Ring Correlation as a Resolution Criterion for Super-Resolution Microscopy.” Journal of Structural Biology 183, no. 3 (2013): 363–367. https://doi.org/10.1016/j.jsb.2013.05.004.*

* *(4) Koho, Sami, Giorgio Tortarolo, Marco Castello, Takahiro Deguchi, Alberto Diaspro, and Giuseppe Vicidomini. “Fourier Ring Correlation Simplifies Image Restoration in Fluorescence Microscopy.” Nature Communications 10, no. 1 (2019). https://doi.org/10.1038/s41467-019-11024-z.*

* *(5) Nieuwenhuizen, Robert P.J., Keith A. Lidke, Mark Bates, Daniela Leyton Puig, David Grünwald, Sjoerd Stallinga, and Bernd Rieger. “Measuring Image Resolution in Optical Nanoscopy.” Nature Methods 10, no. 6 (2013): 557–562. https://doi.org/10.1038/nmeth.2448.*

* *(6) Royer, Loïc A, William C Lemon, Raghav K Chhetri, Yinan Wan, Michael Coleman, Eugene W Myers, and Philipp J Keller. “Adaptive Light-Sheet Microscopy for Long-Term, High-Resolution Imaging in Living Organisms.” Nature Biotechnology, no. October (2016). https://doi.org/10.1038/nbt.3708.*


</div>
