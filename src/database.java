// Henry Darnell
// Database Management Systems
// 010646670

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;

public class database
{

    private static int OVERFLOW_CONSTANT = 2;

    private static int RECORD_SIZE = 71;
    private static int NUM_RECORDS = -1;
    private static int NUM_OVERFLOW = -1;
    private static int foundRecordNumber = -1;
    private static int foundOverflowRecordNumber = -1;

    private static String filename = "";
    private static String foundRecord = "";
    private static String foundOverflowRecord = "";

    private static boolean isOpen = false;

    private static RandomAccessFile Din;
    private static RandomAccessFile Overflow;

    public static void main(String[] args) throws IOException
    {
        Scanner reader = new Scanner(System.in);

        String dbChoice;
        do {
            System.out.println("\nWelcome!");
            System.out.println("Which would you like to do?");
            System.out.println("1) Open existing database");
            System.out.println("2) Create new database");
            System.out.println("Please respond 1 or 2, or quit to exit.");
            dbChoice = reader.next();

            do {
                switch (dbChoice) {
                    case "1":
                        openDB();
                        break;
                    case "2":
                        createDB();
                        break;
                    case "quit":
                    case "Quit":
                    case "q":
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Please answer 1, 2, or quit.");
                        dbChoice = reader.next();
                }
            } while (!dbChoice.equals("1") && !dbChoice.equals("2"));

            if (isOpen) {
                String operationChoice;
                do {
                    System.out.println("\nNow what would you like to do?");
                    System.out.println("1) Display a record by primary key");
                    if (foundRecord.length() > 1 && !foundRecord.equals("NOT_FOUND"))
                        System.out.println("2) Update a record previously displayed by key (Key " + foundRecord.substring(0, 5) + ")");
                    else
                        System.out.println("2) Update a record previously displayed by key (choice 1)");
                    System.out.println("3) Create human readable report (in this directory)");
                    System.out.println("4) Add a new record");
                    System.out.println("5) Delete a record");
                    System.out.println("9) Close database.");

                    System.out.println("Please respond 1 - 5, 9, or quit to exit.");
                    operationChoice = reader.next();
                    switch (operationChoice) {
                        case "1":
                            System.out.print("Enter the primary key to search for: ");
                            String searchKey = String.format("%05d", Integer.parseInt(reader.next()));
                            foundRecord = binarySearch(Din, searchKey);

                            if(searchKey.equals(foundRecord.substring(0, 5))){
                                System.out.println("\nFound key " + searchKey + ":");
                                System.out.println("Id    Experience Married Wage         Industry                        ");
                                System.out.println(foundRecord);
                            } else {
                                foundRecordNumber = -1;
                                foundOverflowRecordNumber = -1;
                                System.out.println("No direct match for key. Closest match: ");
                                System.out.println("Id    Experience Married Wage         Industry                        ");
                                System.out.println(foundRecord);
                            }
                            break;
                        case "2":
                            if (foundRecordNumber > 0){
                                updateRecord(Din, foundRecord);
                            } else if (foundOverflowRecordNumber > 0) {
                                updateRecord(Overflow, foundOverflowRecord);
                            } else {
                                System.out.println("Find an exact record by key first. Choose option 1.");

                            }

                            break;
                        case "3":
                            createReport(Din);
                            break;
                        case "4":
                            addRecord(Din);
                            break;
                        case "5":
                            deleteRecord(Din);
                            break;
                        case "9":
                            closeDB();
                            break;
                        case "quit":
                        case "Quit":
                        case "q":
                            System.exit(0);
                            break;
                        default:
                            System.out.println("Please answer 1 - 5, 9, or quit.");
                            operationChoice = reader.next();
                    }
                } while (!operationChoice.equals("9"));
            }
        } while (!dbChoice.equalsIgnoreCase("quit") && !dbChoice.equalsIgnoreCase("q"));

    }

    private static boolean openDB() throws IOException {
        Scanner reader = new Scanner(System.in);
        System.out.print("Open existing DB :: ");
        filename = reader.next();

        File n = new File(System.getProperty("user.dir") + "/", filename + ".txt");
        if(n.exists()) {
            Din = new RandomAccessFile(filename + ".txt", "rw");
            Overflow = new RandomAccessFile(filename + "_overflow.txt", "rw");
            RandomAccessFile f = new RandomAccessFile(filename + "_config.txt", "rw");
            String config =  f.readLine();
            NUM_RECORDS = Integer.parseInt(config.substring(0, 11).trim());
            NUM_OVERFLOW = Integer.parseInt(config.substring(11).trim());
            f.close();
            isOpen = true;
            return true;
        }


        System.out.println("DB doesn't exist in current directory.");
        isOpen = false;
        return false;
//
    }

    private static boolean createDB(){
        boolean valid = false;
        Scanner reader = new Scanner(System.in);
        while(!valid) {
            System.out.print("What do you want the database file to be called? 16 chars max :: ");
            filename = reader.next();
            if(filename.matches("^[a-zA-Z0-9]*$") && filename.length() < 17)
                valid = true;
            if (!valid)
                System.out.println("Invalid database name, alphanumeric only, 16 chars max");
        }

        File n = new File(System.getProperty("user.dir") + "/", filename + ".txt");
        if(!n.exists()) {

            try {
                RandomAccessFile f = new RandomAccessFile(filename + ".txt", "rw");
                Overflow = new RandomAccessFile(filename + "_overflow.txt", "rw");
                f.writeBytes("id    experience married wage         industry                        \n");
                f.close();

                f = new RandomAccessFile(filename + "_config.txt", "rw");
                f.writeBytes("0          0  "); // num records in all (10 wide), space, num records in overflow 3 wide
                f.close();

                f = new RandomAccessFile(filename + "_overflow.txt", "rw");
                f.writeBytes("");
                f.close();

                NUM_OVERFLOW = 0;
                NUM_RECORDS = 0;
                isOpen = true;

            } catch (FileNotFoundException e) {
                System.out.println("Unable to write file.");
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }
        else {
            System.out.println("DB name is already in use. Try another name.");
            return false;
        }
    }

    private static boolean closeDB(){
        isOpen = false;
        return true;
    }


    private static void createReport(RandomAccessFile Din) throws IOException {
        RandomAccessFile f = new RandomAccessFile(filename + "_report.txt", "rw");
        Din = new RandomAccessFile(filename + ".txt", "rw");
        f.writeBytes("id    experience married wage         industry                        \n");
        String record;
        for (int i = 1; i <= 10; i++) {
            record = getRecord(Din, i);
            f.writeBytes(record + "\n");
        }

        System.out.println("Report written to ./" + filename + "_report.txt.");

    }

    private static void addRecord(RandomAccessFile Din) throws IOException {
        int id = 1;
        String idString;
        int experience = -1;
        String experienceString;
        String married = "no";
        double wage = -1.0;
        String wageString;
        String industry = "";
        String record;
        boolean valid = false;
        boolean taken = true;

        Scanner reader = new Scanner(System.in);
        System.out.println("===========");
        System.out.println("New record:\nFields: id (5 char int), experience (10 char int), married (yes/no), wage (double), industry (32 char string) \n");

        while(!valid || taken) {
            System.out.println("Id (5 char int) : ");
            idString = reader.next();


            if (idString.length() < 6 && idString.matches("\\d+")) {
                valid = true;
                id = Integer.parseInt(idString);
                idString = String.format("%05d", id);

                String temp = binarySearch(Din, idString);

                if (temp.equals("NOT_FOUND") || !temp.substring(0, 5).equals(idString)) {
                    taken = false;
                }


            }
            if (!valid)
                System.out.println("Invalid ID, please enter whole number (5 chars max)");
            else
                if(taken) {
                    System.out.println("ID taken, please enter unique ID");
                }

        }
        valid = false;
        while (!valid) {
            System.out.println("Experience (10 char int) : ");
            experienceString = reader.next();
            if (experienceString.length() < 11 && experienceString.matches("^\\d+$")) {
                experience = Integer.parseInt(experienceString);
                valid = true;
            }
            if (!valid)
                System.out.println("Invalid experience, please enter whole number (10 chars max)");
        }
        valid = false;
        while (!valid) {
            System.out.println("Married (yes/no) : ");
            married = reader.next();
            if(married.equalsIgnoreCase("no") || married.equalsIgnoreCase("yes") || married.equalsIgnoreCase("y") || married.equalsIgnoreCase("n")) {
                if (married.equals("y"))
                    married = "yes";
                if (married.equals("n"))
                    married = "no";
                valid = true;
            }
            if (!valid)
                System.out.println("Invalid married field, please enter yes/no/y/n");
        }
        valid = false;
        while (!valid) {
            System.out.println("Wage (12 char double) : ");
            wageString = reader.next();
            valid = Pattern.matches("-?([0-9]*)\\.([0-9]*)", wageString.trim()) && wageString.trim().length() < 13;

            if (valid)
                wage = Double.parseDouble(wageString);
            if(!valid)
                System.out.println("Invalid wage, please enter a number with decimal (12 char max)");
        }
        reader.skip("\n");
        valid = false;

        while(!valid) {
            System.out.println("Industry (32 char string) : ");
            industry = reader.nextLine();
            if(industry.length() < 33) {
                valid = true;
            }
            if (!valid)
                System.out.println("Invalid industry, please enter a string (32 chars max)");

        }

        experienceString = String.format("%-10s" ,  String.valueOf(experience));
        married =  String.format("%-7s", married);
        wageString = String.format("%-12s", String.valueOf(wage));
        industry = String.format("%-32s", industry);

        RandomAccessFile f = new RandomAccessFile(filename + "_config.txt", "rw");
        RandomAccessFile o = new RandomAccessFile(filename + "_overflow.txt", "rw");
        String idStr = String.format("%05d", id);
        record = idStr + " " + experienceString + " " + married + " " + wageString + " " + industry + "\n";

        o.seek((NUM_OVERFLOW) * RECORD_SIZE);
        o.writeBytes(record);
        NUM_OVERFLOW++;
        Runtime.getRuntime().exec("sort -o " + filename + "_overflow.txt" + " " + filename + "_overflow.txt");

        f.seek(0);
        f.writeBytes(String.format("%-10s" ,  String.valueOf(NUM_RECORDS)) + " " +
                String.format("%-10s" ,  String.valueOf(NUM_OVERFLOW)));

        if(NUM_OVERFLOW  > OVERFLOW_CONSTANT){
            recombine(Din);
        }


    }

    /*Get record number n-th (from 1 to 4360) */
    //public static String getRecord(RandomAccessFile Din, int recordNum) throws IOException 
    private static String getRecord(RandomAccessFile Din, int recordNum) throws IOException
    {
        String record = "NOT_FOUND";
        if ((recordNum >=1) && (recordNum <= NUM_RECORDS))
        {
            Din.seek(0); // return to the top of the file
            Din.skipBytes(recordNum * RECORD_SIZE);
            record = Din.readLine();
        }
        return record;
    }

    private static void deleteRecord(RandomAccessFile Din) throws IOException {
        System.out.println("What is the ID of the record you'd like to delete?");
        Scanner reader = new Scanner(System.in);
        int id = reader.nextInt();
        String deleteId = String.format("%05d", id);
        int deleteRow = binarySearch(deleteId);
        System.out.println("deleteId = " + deleteId);
        System.out.println("deleteRow = " + deleteRow);
        if(deleteRow > 0) {
            Din.seek(deleteRow * RECORD_SIZE);
            System.out.println("Do you want to delete this record? (yes/no) \n" + Din.readLine());
            String answer = reader.next();

            if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y")) {
                Din.seek(deleteRow * RECORD_SIZE);
                Din.writeBytes("-9999");
                System.out.println("Deleted record.");

            } else {
                System.out.println("The record wasn't deleted.");
            }
        }

    }

    // returns true if updated properly
    private static boolean updateRecord(RandomAccessFile Din, String record) throws IOException {
        String temp;
        if (record.equals("NOT_FOUND") || record.length() == 0) {
            System.out.println("Previous record not found, search again to update.");
            return false;
        }

        String id = record.substring(0,5);
        int experience = Integer.parseInt(record.substring(6, 16).trim());
        String married =  record.substring(17, 24).trim();
        double wage = Double.parseDouble(record.substring(25, 37));
        String industry = record.substring(38);
        String wageStr;

        boolean isUpdated = false;
        boolean valid = false;

        while (!isUpdated) {

            Scanner reader = new Scanner(System.in);
            System.out.println("Which field would you like to update?");
            System.out.println("Options: experience / married / wage / industry / cancel (exit)");
            String choice = reader.next();

            switch (choice) {
                case "id":
                    System.out.println("Cannot edit id field.");
                    isUpdated = false;
                    break;
                case "experience":
                    while (!valid) {
                        System.out.println("Enter new years of 'experience' : ");
                        temp = reader.next().trim();
                        if (temp.matches("[0-9]+") && temp.length() < 11 && temp.length() > 0) {
                            experience = Integer.parseInt(temp);
                            if (experience > 0) {
                                isUpdated = true;
                                valid = true;
                            }
                            else
                                System.out.println("Invalid experience! Must be positive");
                        } else {
                            System.out.println("Invalid experience! 10 chars max, positive only, cannot be blank");
                            valid = false;
                        }
                    }
                    break;
                case "married":
                    while (!valid) {
                        System.out.println("Enter new 'married' (no/yes) : ");
                        married = reader.next();
                        if(married.equalsIgnoreCase("no") || married.equalsIgnoreCase("yes") || married.equalsIgnoreCase("y") || married.equalsIgnoreCase("n")) {
                            valid = true;
                            isUpdated = true;
                        }
                        if(!valid)
                            System.out.println("Invalid married field, please enter yes  or no");
                    }
                    break;
                case "wage":
                    while(!valid) {
                        System.out.println("Enter new 'wage' double : ");
                        wageStr = reader.next();
                        valid = Pattern.matches("-?([0-9]*)\\.([0-9]*)", wageStr) && wageStr.trim().length() < 13;

                        if (valid) {
                            isUpdated = true;
                            wage = Double.parseDouble(wageStr);
                        }

                        if (!valid)
                            System.out.println("Invalid wage, please enter a number (with decimal).");
                    }
                    break;
                case "industry":
                    while (!valid) {
                        System.out.println("Enter new 'industry' string : ");
                        reader.skip("\n");
                        industry = reader.nextLine();
                        if(industry.length() < 33) {
                            isUpdated = true;
                            valid = true;
                        }
                        if(!valid)
                            System.out.println("Invalid industry, please enter a string (32 chars max)");
                    }
                    break;
                case "exit":
                case "cancel":
                case "quit":
                    System.out.println("Exiting UPDATE...");
                    isUpdated = true;
                    break;
                default:
                    System.out.println("Unrecognized field, try again.");
                    isUpdated = false;
                    break;
            }
        }


        //format for output
        String expString = String.format("%-10s" ,  String.valueOf(experience));
        married =  String.format("%-7s", married);
        String wageString = String.format("%-12s", String.valueOf(wage));
        industry = String.format("%-32s", industry);

        String newRecord = id + " " + expString + " " + married + " " + wageString + " " + industry;

        if (isUpdated){
            //write
            if(foundRecordNumber > 0)
                Din.seek((foundRecordNumber + 1) * RECORD_SIZE);
            else
                Din.seek(foundOverflowRecordNumber * RECORD_SIZE);
            Din.writeBytes(newRecord);

        }
        System.out.println("record =\n" + newRecord);

        return true;
    }

    /*Binary Search record id, returns line number for deletion*/
    private static int binarySearch(String id) throws IOException
    {
        int Low = 0;
        int High = NUM_RECORDS - 1;
        int Middle = -1;
        String MiddleId;
        if(NUM_RECORDS == 0)
            return -1;

        boolean Found = false;

        while (!Found && (High >= Low))
        {
            Middle = (Low + High) / 2;
            foundRecord = getRecord(Din, Middle + 1);
            foundRecordNumber = Middle;
            MiddleId = foundRecord.substring(0,5);

            int result = MiddleId.compareTo(id);
            if (result == 0)   // ids match
                Found = true;
            else if (result < 0)
                Low = Middle + 1;
            else
                High = Middle -1;
        }
        if(Middle != 0)
            return Middle + 1;
        return -1;
    }

    /*Binary Search record id */
    private static String binarySearch(RandomAccessFile Din, String id) throws IOException
    {
        int Low = 0;
        int High = NUM_RECORDS - 1;
        int Middle;
        String MiddleId;

        if(NUM_RECORDS == 0){
            foundRecord = "NOT_FOUND";
            return foundRecord;
        };

        boolean Found = false;

        while (!Found && (High >= Low))
        {
            Middle = (Low + High) / 2;
            foundRecord = getRecord(Din, Middle + 1);
            foundRecordNumber = Middle;
            if(foundRecord == null) {
                foundRecord = "NOT_FOUND";
                return foundRecord;
            }
            MiddleId = foundRecord.substring(0, 5);

            int result = MiddleId.compareTo(id);
            if (result == 0)   // ids match
                Found = true;
            else if (result < 0)
                Low = Middle + 1;
            else
                High = Middle - 1;
        }
        return foundRecord;
    }

    private static void recombine(RandomAccessFile Din) throws IOException {
        Din.seek(RECORD_SIZE);
        RandomAccessFile f = new RandomAccessFile(filename + "_overflow.txt", "rw");
        RandomAccessFile z = new RandomAccessFile(filename + "_all.txt", "rw");
        z.writeBytes("id    experience married wage         industry                        \n");
        String main = "";
        String overflow = "";
        String mainId;
        String overflowId;

        //Merge sort between sorted Overflow file in f and main database Din
        int x = 0, i = 0;


        while(i < NUM_OVERFLOW && x < NUM_RECORDS){
            if(main.equals(""))
                main = Din.readLine();
            if(overflow.equals(""))
                overflow = f.readLine();
            mainId = main.substring(0, 5);
            overflowId = overflow.substring(0, 5);

            if(mainId.compareTo(overflowId) < 0) {

                if(main.substring(0,2).compareTo("0") < 0) {
                    main = "";
                    continue;
                }

                //Main is less than overflow
                z.writeBytes(main + "\n");
                x++;
                main = "";
            } else {
                //Overflow is less than main
                z.writeBytes(overflow + "\n");
                i++;
                overflow = "";
            }
        }

        // one file ran out of records!
        while (i < NUM_OVERFLOW ){

            if(overflow != null)
                z.writeBytes(overflow + "\n");
            i++;
            overflow = f.readLine();
        }
        while (x < NUM_RECORDS ){

            if(main != null)
                z.writeBytes(main + "\n");
            x++;
            main = Din.readLine();
        }
        NUM_RECORDS += NUM_OVERFLOW;
        NUM_OVERFLOW = 0;
        RandomAccessFile q = new RandomAccessFile(filename + "_config.txt", "rw");
        q.seek(0);
        q.writeBytes(String.format("%-10s" ,  String.valueOf(NUM_RECORDS)) + " " +
                String.format("%-10s" ,  String.valueOf(NUM_OVERFLOW)));

        Runtime.getRuntime().exec("cat " + filename + "_all.txt" + " > " + filename + ".txt");

        ProcessBuilder builder = new ProcessBuilder("cat", filename + "_all.txt");
        builder.redirectOutput(new File(filename + ".txt"));
        builder.redirectError(new File(filename + ".txt"));
        Process p = builder.start();

        Runtime.getRuntime().exec("rm " + filename + "_all.txt");

        FileWriter a = new FileWriter(filename + "_overflow.txt", false);
        PrintWriter b = new PrintWriter(a, false);
        b.flush();
        b.close();
        a.close();



    }

}