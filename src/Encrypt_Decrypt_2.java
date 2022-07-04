
import java.util.Random;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author hp
 */ 

public class Encrypt_Decrypt_2 {
    
    int generateRandomNumber()
    {
      Random rand = new Random(); 
      int key = rand.nextInt(100);
      return key;
    }
    
    public String encryption(String data, int key)
    {
        StringBuilder encrypt = new StringBuilder();
        int length_finder = data.length();
        //System.out.println(length_finder);
        int i;
        char new_char;
        for(i=0;i<length_finder;i++)
        {
            new_char = (char)(((int)data.charAt(i) + key));
            encrypt.append(new_char);
        }
        return encrypt.toString();
    }
    
    public String decryption(StringBuilder data, int key)
    {
        StringBuilder decrypt = new StringBuilder();
        int length_finder = data.length();
        int i;
        char new_char;
        for(i=0;i<length_finder;i++)
        {
            new_char = (char)(((int)data.charAt(i) - key));
            decrypt.append(new_char);
        }
        return decrypt.toString();
    }
}
