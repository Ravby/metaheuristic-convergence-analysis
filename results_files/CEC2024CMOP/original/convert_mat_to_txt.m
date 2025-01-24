% Get the directory of the current script
scriptDir = fileparts(mfilename('fullpath'));

% Define the folder containing the .mat files
folderPath = fullfile(scriptDir, 'CCEMT'); %CCEMT CCPTEA DESDE IMTCMO MTCMMO

% Get a list of all .mat files in the folder
matFiles = dir(fullfile(folderPath, '*.mat'));

% for penalizing constraint violations
LARGE_NUMBER = 10^8;

% Loop through each .mat file
for k = 1:length(matFiles)
    % Get the full path to the .mat file
    matFilePath = fullfile(folderPath, matFiles(k).name);
       
    % Load the .mat file
    loadedData = load(matFilePath);
    
    % Dynamically retrieve the variable name
    variableNames = fieldnames(loadedData);
    if isempty(variableNames)
        error('The file %s contains no variables.', matFiles(k).name);
    end
    
    % Assume the first variable is the data matrix
    IGD_MCV = loadedData.(variableNames{1});

    % Define the output .txt file path
    [~, fileName, ~] = fileparts(matFiles(k).name);
    txtFilePath = fullfile(scriptDir, [fileName, '.txt']);

    % Open the .txt file for writing
    fileID = fopen(txtFilePath, 'w');

    % Write the IGD array to the .txt file, if MCV is grater than 0, add
    % 10e8 to MCV and write that instead
    [rows, cols] = size(IGD_MCV);
    for i = 1:rows
        for j = 1:2:cols % Process IGD and MCV in pairs
            IGD = IGD_MCV(i, j);
            MCV = IGD_MCV(i, j+1);

            if MCV > 0
                fprintf(fileID, '%.10f ', LARGE_NUMBER + MCV); 
            else
                fprintf(fileID, '%.10f ', IGD); 
            end
        end
        fprintf(fileID, '\n');
    end

    fclose(fileID);
end
