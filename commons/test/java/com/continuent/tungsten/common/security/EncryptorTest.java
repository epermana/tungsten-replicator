/**
 * Tungsten Scale-Out Stack
 * Copyright (C) 2007-2009 Continuent Inc.
 * Contact: tungsten@continuent.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of version 2 of the GNU General Public License as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
 *
 * Initial developer(s): Ludovic Launer
 */

package com.continuent.tungsten.common.security;

import java.text.MessageFormat;

import com.continuent.tungsten.common.jmx.AuthenticationInfo;

import junit.framework.TestCase;

/**
 * Implements a simple unit test for AuthenticationInfo
 * 
 * @author <a href="mailto:ludovic.launer@continuent.com">Ludovic Launer</a>
 * @version 1.0
 */
public class EncryptorTest extends TestCase
{
    /**
     * Tests encryption / decryption
     */
    public void testIsAuthenticationNeeded() throws Exception
    {
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setKeystore("tungsten_sample_keystore.jks", "secret");
        authInfo.setTruststore("tungsten_sample_truststore.ts", "secret");
        
        Encryptor encryptor = new Encryptor(authInfo);
        
        String testString = "This is a test string. It will be a password";
        
        String encryptedString = encryptor.encrypt(testString);
        
        String decryptedString = encryptor.decrypt(encryptedString);
        
        assertEquals(testString, decryptedString);

    }

   
    

}