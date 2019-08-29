package de.hbznrw.ygor.validators

import de.hbznrw.ygor.enums.*
import de.hbznrw.ygor.export.structure.TitleStruct
import de.hbznrw.ygor.bridges.*
import de.hbznrw.ygor.normalizers.DateNormalizer
import de.hbznrw.ygor.tools.UrlToolkit
import org.apache.commons.lang.StringUtils
import org.apache.commons.validator.routines.UrlValidator

import java.sql.Timestamp

// checks if given value meets the requirements

class Validator {

    static UrlValidator URL_VALIDATOR = new UrlValidator(["http","https"] as String[])

    static validate(String type, String value, String... additionalParameters){
        switch (type) {
            case "Title":
                return isValidString(value)
                break
            case "String":
                return isValidString(value)
                break
            case "Number":
                return isValidNumber(value)
                break
            case "ID":
                return isValidIdentifier(value,
                                         additionalParameters[0] /* must contain ID type */,
                                         additionalParameters[1] /* must contain namespace */)
                break
            case "URL":
                return isValidURL(value)
                break
            case DateNormalizer.START_DATE:
                return isValidDate(value)
                break
            case DateNormalizer.END_DATE:
                return isValidDate(value)
                break
            case "Date":
                return isValidDate(value)
                break
            case "ISBN":
                return validateISBN(value)
                break
            default:
                return Status.UNDEFINED
        }
    }
    
    /**
     *
     * @param str
     * @return
     */
    static isValidString(String str) {
        if(!str || str.trim().equals("")){
            return Status.VALIDATOR_STRING_IS_MISSING
        }
        else if(str.length() < 2){
            if (!StringUtils.isNumeric(str)){
                return Status.VALIDATOR_STRING_IS_INVALID
            }
        }
        else if(str.contains("|")){
            return Status.VALIDATOR_STRING_IS_NOT_ATOMIC
        }
        return Status.VALIDATOR_STRING_IS_VALID
    }
    
    /**
     *
     * @param str
     * @return
     */
    static isValidNumber(String str) {
        if(!str || str.trim().equals("")){
            return Status.VALIDATOR_NUMBER_IS_MISSING
        } 
        else if(str.contains("|")){
            return Status.VALIDATOR_NUMBER_IS_NOT_ATOMIC
        }
        else if(str.isInteger()){
            return Status.VALIDATOR_NUMBER_IS_VALID
        }
        
        return Status.VALIDATOR_NUMBER_IS_INVALID
    }
    
    /**
     * 
     * @param str
     * @param identifierType
     * @return
     */
    static isValidIdentifier(String str, Object identifierType, String namespace) {
        if (!str || str.trim().equals("")) {
            return Status.VALIDATOR_IDENTIFIER_IS_MISSING
        }
        if (str.contains("|")) {
            return Status.VALIDATOR_IDENTIFIER_IS_NOT_ATOMIC
        }
        if (identifierType in ["onlineIdentifier", "printIdentifier"]) {
            if (str.matches("\\d{4}-\\d{4}")){
                return Status.VALIDATOR_IDENTIFIER_IS_VALID
            }
            else {
                return Status.VALIDATOR_IDENTIFIER_IS_INVALID
            }
        }
        else if (identifierType.equals(ZdbBridge.IDENTIFIER)) {
            if (2 < str.length() && str.indexOf("-") == str.length() - 2) {
                return Status.VALIDATOR_IDENTIFIER_IS_VALID
            } else {
                return Status.VALIDATOR_IDENTIFIER_IS_INVALID
            }
        }
        else if (identifierType.equals(TitleStruct.EISBN) || identifierType.equals(TitleStruct.PISBN)) {
            if (validateISBN(str)) {
                return Status.VALIDATOR_IDENTIFIER_IS_VALID
            } else {
                return Status.VALIDATOR_IDENTIFIER_IS_INVALID
            }
        }
        else if (identifierType.equals(EzbBridge.IDENTIFIER)) {
            // TODO .. no valid definition 
            if (str.length() > 2) {
                return Status.VALIDATOR_IDENTIFIER_IS_VALID
            } else {
                return Status.VALIDATOR_IDENTIFIER_IS_INVALID
            }
        }
        else if (identifierType.equals(TitleStruct.DOI)) {
            if (str.startsWith("10.")) {
                return Status.VALIDATOR_IDENTIFIER_IS_VALID
            } else {
                return Status.VALIDATOR_IDENTIFIER_IS_INVALID
            }
        }
        else if (identifierType.equals("inID_" + namespace)) {
            if (str) {
                return Status.VALIDATOR_IDENTIFIER_IS_VALID
            } else {
                return Status.VALIDATOR_IDENTIFIER_IS_INVALID
            }
        }
        else if (identifierType == namespace){
            // TODO use identifier type in GOKb (String, URL, ...) and specify here
            return Status.VALIDATOR_IDENTIFIER_IS_VALID
        }
        return Status.VALIDATOR_IDENTIFIER_IN_UNKNOWN_STATE
    }
    
    
    /**
     * 
     * @param str
     * @return
     */
    static isValidURL(String str) {
        if (StringUtils.isEmpty(str)){
            return Status.VALIDATOR_URL_IS_MISSING
        }
        else if(str.contains("|")){
            return Status.VALIDATOR_URL_IS_NOT_ATOMIC
        }
        if (URL_VALIDATOR.isValid(str)){
            return Status.VALIDATOR_URL_IS_VALID
        }
        return Status.VALIDATOR_URL_IS_INVALID
    }
    
    /**
     * 
     * @param str
     * @return
     */
    static isValidDate(String str) {
        if(!str || str.trim().equals("")){
            return Status.VALIDATOR_DATE_IS_MISSING
        }
        try {
            def check = Timestamp.valueOf(str)
            return Status.VALIDATOR_DATE_IS_VALID
        }
        catch(Exception e) {
            return Status.VALIDATOR_DATE_IS_INVALID
        }
    }

    /**
    * Validate ISBN 13
    * https://www.moreofless.co.uk/validate-isbn-13-java/
    *
    * @param str
    * @return
    */

    static boolean validateISBN(String str) {
        def isbn = str
        if ( isbn == null ) {
            return false
        }
        //remove any hyphens
        isbn = isbn.replaceAll( "-", "" )

        //must be a 13 digit ISBN
        if ( isbn.length() != 13 ) {
            return false
        }
        try {
            int tot = 0;
            for ( int i = 0; i < 12; i++ )
            {
                int digit = Integer.parseInt( isbn.substring( i, i + 1 ) )
                tot += (i % 2 == 0) ? digit * 1 : digit * 3
            }
            //checksum must be 0-9. If calculated as 10 then = 0
            int checksum = 10 - (tot % 10)
            if ( checksum == 10 )
            {
                checksum = 0
            }
            return checksum == Integer.parseInt( isbn.substring( 12 ) )
        }
        catch ( NumberFormatException nfe ) {
            //to catch invalid ISBNs that have non-numeric characters in them
            return false
        }
    }
}