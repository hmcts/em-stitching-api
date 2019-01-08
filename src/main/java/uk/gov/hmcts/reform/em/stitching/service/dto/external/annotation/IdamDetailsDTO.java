package uk.gov.hmcts.reform.em.stitching.service.dto.external.annotation;

public class IdamDetailsDTO {

    private String forename;

    private String surname;

    private String email;

    public IdamDetailsDTO() {}

    public IdamDetailsDTO(String forename, String surname) {
        this.forename = forename;
        this.surname = surname;
    }

    public String getForename() {
        return forename;
    }

    public void setForename(String forename) {
        this.forename = forename;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
