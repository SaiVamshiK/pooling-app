package com.example.miniproject;

public class DriverCustomFields {
    String driveremail,driverpassword,driverusername;

    public DriverCustomFields(String driveremail, String driverpassword, String driverusername) {
        this.driveremail = driveremail;
        this.driverpassword = driverpassword;
        this.driverusername = driverusername;
    }

    public String getDriveremail() {
        return driveremail;
    }

    public void setDriveremail(String driveremail) {
        this.driveremail = driveremail;
    }

    public String getDriverpassword() {
        return driverpassword;
    }

    public void setDriverpassword(String driverpassword) {
        this.driverpassword = driverpassword;
    }

    public String getDriverusername() {
        return driverusername;
    }

    public void setDriverusername(String driverusername) {
        this.driverusername = driverusername;
    }
}
