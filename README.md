# Google Location History Map Generator
This tools lets you generate a series of maps images of a road trip, excursion, etc. You can feed it your Google location history and, using Google Maps static APIs, it will produce a series of images.

## Example
An example image:

![Example](https://github.com/MMauro94/glhmg/exampleMap.png)

## Step-by-step guide

### 1. Download you Google Location History 
1. Head to https://takeout.google.com/settings/takeout
2. Deselect everything except "Location History" and be sure that the format is in JSON
3. Click "Next"
4. Select how to receive the archive and click "Create archive"
5. Wait for it to complete, download it and extract the location history JSON file to a known location

### 2. Obtain a Google Maps static API key
1. Head to https://developers.google.com/maps/documentation/static-maps/get-api-key
2. Obtain an API key
3. Save it somewhere safe

### 3. TODO other steps