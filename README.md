# AR Sudoku Solver

This project is a Sudoku solver application for Android in augmented reality (AR).  
It uses OpenCV for all image processing.

## How to build

Clone this repo and add in the ARSudokuSolver folder a `gradle.properties` file.  
There should be in this file the following values:  
```
sdk.dir=/path/to/Android/Sdk
signings.store.path=/path/to/keystore/file.jks
signings.store.password=keystore_password
signings.key.alias=key_name
signings.key.password=key_password
```  
Then you should be able to open the folder containing this file in Android Studio


## Pipeline

### Step 1

I use the camera to get the picture.  
![Image from camera](./pictures/step1.jpg)

### Step 2

I use the adaptativeThreshold function to highlight the contours.  
![Results of the adaptative threshold](./pictures/step2.jpg)

### Step 3

I use the findContours function to find the maximum quadrilateral area of the picture. Then I show the grid in the image.  
![Grid shown in the picture](./pictures/step3.jpg)

### Step 4

I use the getPerspectiveTransform function to find the transformation to get a square from the quadrilateral. The I use the warpPerspectiveFunction to apply the transformation on the thresholded image.  
<img src="./pictures/step4.jpg" alt="Grid shown in the picture" width="480px">

### Step 5

I divide the image into 81 images for each cell of the grid, after are some exemples.  
Then I use a neural network to deduce the number of each cell (or 0 if there is no number).  
<img src="./pictures/1.jpg" alt="Little square" width="80px">
<img src="./pictures/2.jpg" alt="Little square" width="80px">
<img src="./pictures/3.jpg" alt="Little square" width="80px">
<img src="./pictures/4.jpg" alt="Little square" width="80px">
<img src="./pictures/5.jpg" alt="Little square" width="80px">
<img src="./pictures/6.jpg" alt="Little square" width="80px">  

### Step 6

I use an algorithm (explained after) to solve the sudoku. Then, I write each values to the square image.  
<img src="./pictures/step5.jpg" alt="Completed Sudoku" width="480px">

### Step 7

I use the transformation from the square to the quadrilateral made of the contours of the detected grid to project the numbers at the place of the grid.  
![Completed sudoku projected](./pictures/step6.jpg)

### Step 8

Finally I substract the image with the numbers to the original photo to make appear the numbers in black.  
![Final image](./pictures/step7.jpg)

## Sudoku Resolution

To solve the sudoku, I repeat the following steps until the end.
- I find all possible values for each empty cell
- If a cell has only one possible value
	- I affect that value to the cell
- If no value was affected
	- I choose one cell with the least number of possible values
	- I choose one of the possible value and affect it, and save that choice
- If there is an error
	- Remove all values affected since the last saved choice
	- Remove that choice of the possible values

## Neural Network

About this, I'm still working on it because I have some problems when filming a screen because of the Moiré pattern that appears on the picture.  
I don't know yet how to generate the Moiré pettern noise nor how to remove it from picture.  
During the threshold operation, it makes appear a lot of noise hence it is difficult to the AI to find the right number in the cell.  
I have then 2 possibilities:  
- Generate noisy images with Moiré pattern noise to denoise using another neural network
- Denoise the Moiré pattern before using the neural network

### Generate the Moiré pattern noise

I tried to generate the noise using thresholded Perlin noise with different parameters, following picture give the result I have.  
<img src="./pictures/perlin_noise_280x280_10octaves.png" alt="Perlin noise" width="480px">  
I want to use this noise to improve the dataset, but for now it doesn't work.

### Remove the Moiré pattern noise

I tried to remove this pattern directly from the original picture.  
It appears that method like blocking some frequencies using the Fourier transform doesn't work as well as expected, the following pictures show some results.  

- Input image  
<img src="./pictures/denoising/input.jpg" alt="input" width="480px">

- Fourier transform result  
![Fourier_result](./pictures/denoising/Fourier_result.png)

- Band stop filter  
![band_stop_filter](./pictures/denoising/band_stop_filter.png)

- Band stop result  
![band_stop_result](./pictures/denoising/band_stop_result.png)

- Cut filter  
![cut_filter](./pictures/denoising/cut_filter.png)

- Cut result  
![cut_result](./pictures/denoising/cut_result.png)
