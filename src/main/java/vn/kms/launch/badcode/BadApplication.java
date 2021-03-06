package vn.kms.launch.badcode;

import java.io.*;
import java.util.*;

public class BadApplication {
  private static final Set<String> m_valid_state_codes = new HashSet<>(Arrays.asList("AL","AK","AS","AZ","AR","CA","CO","CT","DE","DC","FL","GA","GU","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MH","MA","MI","FM","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","MP","OH","OK","OR","PW","PA","PR","RI","SC","SD","TN","TX","UT","VT","VA","VI","WA","WV","WI","WY"));
  private static int year_of_Report = 2016;
  private static final Map<String, String> report_headers = new HashMap<String, String>(){{
    put("invalid-contact-details","contact_id\terror_field\terror_message\r\n");
    put("invalid-contact-summary","field_name\tnumber_of_invalid_contact\r\n");
    put("contact-per-state","state_code\tnumber_of_contact\r\n");
    put("contact-per-age-group","group\tnumber_of_contact\tpercentage_of_contact\r\n");
  }};

  public static void main(String[] args) throws Exception {
    Map<String, Integer> field_error_counts = new TreeMap<>(); // I want to sort by key
    Map<Integer, Map<String, String>> invalidContacts = new LinkedHashMap<>(); // invalidContacts order by ID
    Map reports = loadFileAndConvertToEntityAndValidateEntityAndStoreEntityAndReturnReports(field_error_counts, invalidContacts);

    // 5. Store reports
    for(Object object:report_headers.keySet()) {
      String reportName = (String) object;
      File outputFile = new File("output");
      if (!outputFile.exists()) {
        outputFile.mkdirs();
      }

      Writer writer = new FileWriter(new File(outputFile, reportName + ".tab"));

      // write header
      writer.write(report_headers.get(reportName));
      
      Map<String, Integer> report = (Map<String, Integer>) reports.get(reportName);
      if(reportName.equals("contact-per-age-group")){
    	  int total=0;
          for(Integer v: report.values()) {
        	  total+=v;
          }
        // I want to sort by age-group followed the requirement
        String[] age_groups = { "Children", "Adolescent", "Adult", "Middle Age", "Senior" };
          for(String item:age_groups) {
              writer.write(item + "\t" + report.get(item) +"\t" + (int)((report.get(item) * 100.0f)/total) +"%\r\n");
            }
      } else if(reportName.equals("contact-per-state")){
        report = new TreeMap<>(report); // I want to sort by state
	      for(String item:report.keySet()) {
	        writer.write(item + "\t" + report.get(item) + "\r\n");
	      }
      } else if(reportName.equals("invalid-contact-summary")){
        for (Map.Entry entry : field_error_counts.entrySet()) {
          writer.write(entry.getKey() + "\t" + entry.getValue() + "\r\n");
        }
      }
      else { //invalid-contact-details
        for (Map.Entry<Integer, Map<String, String>> entry : invalidContacts.entrySet()) {
          int contactID = entry.getKey();
          Map<String, String> error_by_fields = entry.getValue();
          for (String field_name : error_by_fields.keySet()) {
            writer.write(contactID + "\t" + field_name + "\t" + error_by_fields.get(field_name) + "\r\n");
          }
        }
      }
      writer.flush();
      System.out.println("Generated report " + "output/" + reportName + ".tab");
    }
  }

  /**
   * @return array of total-lines, blank-lines, invalid-lines, valid-entities, invalid-entities, total-errors
   */
  private static Map loadFileAndConvertToEntityAndValidateEntityAndStoreEntityAndReturnReports(Map<String, Integer> counts, Map<Integer, Map<String, String>> invalidContacts) throws Exception {
    // 1. Load data from file
    InputStream is = new FileInputStream("data/contacts.tsv");
    char[] buff = new char[100000]; // guest file size is not greater than 100000 chars
    int b; // read one character
    int c = 0; // count total characters in file
    while ((b = is.read()) != -1) {
      buff[c] = (char) b;
      c++;
    }

    String s = new String(buff, 0, c); // all data from file load to string s
    String[] lines = s.split("\r"); // get all lines
    List<Contact> allContacts = new ArrayList<>();
    int blankCount = 0;
    int invalidLineCount = 0;
    for (int i=0; i<lines.length; i++) {
      // debug
      // System.out.println(lines[i]);

      if (i == 0) { // ignore header line
        continue;
      }

      // count blank line by trimming space characters and then check length
      if (lines[i].trim().length() == 0) {
        blankCount++;
        continue;
      }

      String[] data = lines[i].split("\t"); // get data of a line

      if (data.length != 14) { // invalid line format
        invalidLineCount++;
        continue;
      }

      // TODO: I will use refection & annotations to build contact object later
      Contact contact = new Contact();

      try {
        // first column is ID
        // FIXME: Don't forget to change this code if first column is not ID
        contact.setId(Integer.parseInt(data[0]));
      } catch (Exception ex) {
        // log error
        System.out.println("It is not ID format");
        invalidLineCount++;
        continue;
      }

      // FIXME: need to change this code if the order of column may be changed
      contact.setFirst_name(data[1]); // first_name
      contact.setLsName(data[2]); // last_name
      contact.day_of_birth = data[4]; // day_of_birth
      contact.setAddress(data[5]); // city
      contact.setCity(data[6]); // city
      contact.setSTATE(data[8]); // state
      contact.setZ_Code(data[9]); // zip
      contact.setMobile_Phone(data[10]); // phone1
      contact.setEmail(data[12]); // email

      if (contact != null) {
        allContacts.add(contact);
      } else {
        // for some reason, I think contact object may be null (I am not sure, but I want to log for sure!!!)
        System.out.println("Contact is null, don't know why!!!");
      }
    }

    // 2. Validate contact data
    // TODO: I will use refection & annotations to isValid contact data later
    for (Contact contact : allContacts) {
      Map<String, String> errors = new TreeMap<>(); // errors order by field name
      if (contact.getFirst_name().trim().length() == 0) {
        errors.put("firstName", "is empty");
        add_FieldERROR(counts, "first_name");
      }
      if (contact.getFirst_name().length() > 10) {
        errors.put("firstName", "'" + contact.getFirst_name() + "''s length is over 10");
        add_FieldERROR(counts, "first_name");
      }
      if (contact.getLsName().trim().length() == 0) {
        errors.put("lastName", "is empty");
        add_FieldERROR(counts, "last_name");
      }
      if (contact.getLsName().length() > 10) {
        errors.put("lastName", "'" + contact.getLsName() + "''s length is over 10");
        add_FieldERROR(counts, "last_name");
      }
      if (contact.day_of_birth == null || contact.day_of_birth.trim().length() != 10) {
        errors.put("day_of_birth", "'" + contact.day_of_birth + "' is invalid");
        add_FieldERROR(counts, "day_of_birth");
      }
      if (contact.getAddress().length() > 20) {
        errors.put("address", "'" + contact.getAddress() + "''s length is over 20");
        add_FieldERROR(counts, "address");
      }
      if (contact.getCity().length() > 15) {
        errors.put("city", "'" + contact.getCity() + "''s length is over 15");
        add_FieldERROR(counts, "city");
      }
      if (!m_valid_state_codes.contains(contact.getSTATE())) {
        errors.put("state", "'" + contact.getSTATE() + "' is incorrect state code");
        add_FieldERROR(counts, "state");
      }
      if (!contact.getZ_Code().matches("^\\d{4,5}$")) {
        errors.put("zipCode", "'" + contact.getZ_Code() + "' is not four or five digits");
        add_FieldERROR(counts, "zip_code");
      }
      if (!contact.getMobile_Phone().matches("^\\d{3}\\-\\d{3}\\-\\d{4}$")) {
        errors.put("mobilePhone", "'" + contact.getMobile_Phone() + "' is invalid format XXX-XXX-XXXX");
        add_FieldERROR(counts, "mobile_phone");
      }
      if (!contact.getEmail().matches("^.+@.+\\..+$")) {
        errors.put("email", "'" + contact.getEmail() + "' is invalid email format");
        add_FieldERROR(counts, "email");
      }

      if (!errors.isEmpty()) {
        invalidContacts.put(contact.getId(), errors);
      } else { // populate other fields from raw fields
        contact.setAge(calculate_age_by_year(contact.day_of_birth)); // age
      }
    }

    // 3. Sort contact by zipcode
    // TODO: need to change this code if we use other field to sort the list
    for(int i=0; i<allContacts.size(); i++) {
      for (int j = allContacts.size() - 1; j > i; j--) {
        Contact contact_a = allContacts.get(i);
        Contact contact_b = allContacts.get(j);
        if (!invalidContacts.containsKey(contact_a.getId()) && !invalidContacts.containsKey(contact_b.getId())) {
          int zip_a = Integer.parseInt(contact_a.getZ_Code());
          int zip_b = Integer.parseInt(contact_b.getZ_Code());
          if (zip_a > zip_b) {
            allContacts.set(i, contact_b);
            allContacts.set(j, contact_a);
          }
        }
      }
    }

    // 4. Store valid data
    File outputFile = new File("output");
    if (!outputFile.exists()) {
      outputFile.mkdirs();
    }
    Writer writer1 = new FileWriter(new File(outputFile,"valid-contacts.tab"));
    // write header
    writer1.write("id\tfirst_name\tlast_name\tday_of_birth\taddress\tcity\tstate\tzip_code\tmobile_phone\temail\r\n");
    for (Contact contact : allContacts) {
      if (!invalidContacts.containsKey(contact.getId())) {
        writer1.write(contact.toLine() + "\r\n");
      }
    }
    writer1.flush();

    // 5. Generate contact per state report
    Map reports = new HashMap<>();
    Map<String, Integer> r_contact_per_state = new HashMap<>();
    Map<String, Integer> r_contact_per_age_group = new HashMap<>();
    for (Contact contact : allContacts) {
      if (!invalidContacts.containsKey(contact.getId())) {
        int state_count = 0;
        if(r_contact_per_state.containsKey(contact.getSTATE())) {
          state_count = r_contact_per_state.get(contact.getSTATE());
        }
        r_contact_per_state.put(contact.getSTATE(), state_count + 1);

        int age_group_count = 0;
        if(r_contact_per_age_group.containsKey(calculate_age_group(contact.getAge()))) {
          age_group_count = r_contact_per_age_group.get(calculate_age_group(contact.getAge()));
        }
        r_contact_per_age_group.put(calculate_age_group(contact.getAge()), age_group_count + 1);
      }
    }
    
    reports.put("contact-per-state", r_contact_per_state);
    reports.put("contact-per-age-group", r_contact_per_age_group);
    return reports;
  }

  private static void add_FieldERROR(Map<String, Integer> counts, String field_name) {
    Integer count = counts.get(field_name);
    if (count == null) {
      count = new Integer(0);
    }
    count = count + 1;
    counts.put(field_name, count);
  }

  /**
   * Calculate the age of contact by year. It's not accurate but acceptable
   * @param date_of_birth
   * @return
     */
  public static int calculate_age_by_year(String date_of_birth){
    String yearStr = date_of_birth.split("/")[2];
    int year = Integer.parseInt(yearStr);
    return year_of_Report - year;
  }
  
  // TODO: Calculate age exactly by month/day/year.
  public static int precise_calculate_age(String date_of_birth) {
    return 10;
  }

  private static String calculate_age_group(int age){
    if(age<=9){
      return "Children";
    } else if(age<19) {
      return "Adolescent";
    } else if(age<=45) {
      return "Adult";
    } else if(age<=60) {
      return "Middle Age";
    } else {
      return "Senior";
    }
  }
}
