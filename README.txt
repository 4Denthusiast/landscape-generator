This procedurally generates landscapes, either on a square torus or on a sphere. The current features of the 2 versions are somewhat different. Running the program without arguments runs the torus version, and rose.sh runs the sphere version.
The view of the globe version can be rotated by dragging the mouse, and set to rotate at a constant pace by pressing space while dragging.
In the torus version, the generation can be done in steps with the buttons at the bottom of the control panel. The checkboxes control which features of the landscape are displayed. The save button outputs the generated landscape as a series of images (intended to be viewed as layers, but the PNG format doesn't support layered images). I haven't had an opportunity to test this on Windows, but I've heard it doesn't work.
All of the parameters other than which version to run are passed in through the config file. The supported options are:
    size: the edge length of the torus version in pixels (power of 2)
    weighting: the roughness of the terrain (0.5< w <1 is a reasonable range)
    evaporation: this controls how much water there is, with higher values giving smaller lakes (must be >1)
    rose size: the number of points in the sphere version (must be at least 16, may run out of graphics memory and behave oddly over a few * 100000)
    seed: seed for all of the random parts of the generation, chosen randomly each time if not specified.
