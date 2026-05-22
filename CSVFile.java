import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;

public class CSVFile {

    private String filePath;
    private int rowCount;
    private int columnCount;
    private int currentRow;
    private int failedRowCount;
    private int failedRowValue;

    private String[][] data;
    private String[][] wrongData;
    private boolean[] structurallyValid;
	private String[] headers;

    public CSVFile(String filePath) {
        this.filePath = filePath;
    }

    public void load() {

        rowCount = 0;
        currentRow = 0;
        failedRowCount = 0;
        failedRowValue = 0;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;

            line = reader.readLine();
            if (line == null || line.replace("\uFEFF", "").trim().isEmpty()) {
                reader.close();
                System.out.println("File is Empty");
                return;
            }

            // Remove trailing comma from header
            line = line.trim();
            if (line.endsWith(",")) {
                line = line.substring(0, line.length() - 1);
            }

            headers = line.split(",", -1);
            columnCount = headers.length;

            while ((line = reader.readLine()) != null) {
                rowCount++;
            }
            reader.close();

            data = new String[rowCount][columnCount];
            wrongData = new String[rowCount][columnCount];
            structurallyValid = new boolean[rowCount];

            reader = new BufferedReader(new FileReader(filePath));
            reader.readLine();

            while ((line = reader.readLine()) != null) {

                // Remove trailing comma from data rows
                line = line.trim();
                if (line.endsWith(",")) {
                    line = line.substring(0, line.length() - 1);
                }

                String[] tempo = line.split(",", -1);

                // Check if columns don't match (too many OR too few)
                if (tempo.length != columnCount) {
                    structurallyValid[currentRow] = false;
                    failedRowCount++;

                    for (int i = 0; i < Math.min(tempo.length, columnCount); i++) {
                        wrongData[failedRowValue][i] = tempo[i];
                    }

                    failedRowValue++;

                } else {
                    structurallyValid[currentRow] = true;

                    for (int i = 0; i < columnCount; i++) {
                        data[currentRow][i] = tempo[i];
                    }
                }

                currentRow++;
            }

            reader.close();

        } catch (Exception e) {
            System.out.println("Error loading CSV:");
            e.printStackTrace();
        }
    }

    // =============================
    // GETTERS
    // =============================

    public int getTotalRows() {
        return currentRow;
    }

    public int getFailedRowCount() {
        return failedRowCount;
    }

    public int getValidRowCount() {
        return currentRow - failedRowCount;
    }

    public boolean[] getValidationStatus() {
        return structurallyValid;
    }

    public String[][] getCleanData() {
        return data;
    }

    public String[][] getDirtyData() {
        return wrongData;
    }
	
	public String[] getHeaders(){
		return headers;
	}
}