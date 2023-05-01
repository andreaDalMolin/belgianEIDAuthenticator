package com.beid.belgianeidauth.model;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.beid.belgianeidauth.util.RandomChallengeGenerator;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Base64;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import static com.beid.belgianeidauth.util.CryptoUtil.decryptStringWithAES;
import static com.beid.belgianeidauth.util.CryptoUtil.encryptStringWithAES;

public class Client {
    private static final int SERVER_PORT = 5000;
    private static final String SERVER_IP = "127.0.0.1";
    private static final String CERT_PASSWD = "dalgov";
    private static final int SESSION_KEY_LENGTH = 32;
    private static final String ENCRYPTION_ALGORITHM = "ECIES";
    private static final String ENCRYPTION_PROVIDER = "BC";
    private static final Logger LOGGER = LogManager.getLogger(Client.class);
    private static PublicKey publicKey = null;
    private static PrivateKey privateKey = null;
    private static boolean cardIsLoaded = false;
    EIDCardManager eidCard;

    public void loadCard() {
        while (true) {
            try {
                eidCard = new EIDCardManager();
                publicKey = eidCard.getPublicKey();
                privateKey = eidCard.getPrivateKey();
                cardIsLoaded = true;
                LOGGER.error("Card detected !");
            } catch (Exception e) {
                cardIsLoaded = false;
                LOGGER.error("No card detected");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting for card to be detected");
            }
        }
    }

    public boolean isCardLoaded() {
        return cardIsLoaded;
    }

    public static void getBankAuthorization(String clientNumber, String pin) {
        try {
            //TODO Fetch bank's certificate from Certificate Server
            Security.addProvider(new BouncyCastleProvider());
            PublicKey bankPubKey = getBankPubKey();

            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                 InputStream in = socket.getInputStream();
                 ObjectInputStream inputStream = new ObjectInputStream(in);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                // 1. Generate a session key and sign it
                byte[] sessionKey = RandomChallengeGenerator.generateRandomChallenge(SESSION_KEY_LENGTH);
                byte[] signature = signMessage(sessionKey, privateKey);

                // 2. Send Session key + Signature + EID Public Key to the bank
                String sessionKeyAndSignature =
                        Base64.getEncoder().encodeToString(signature)
                                + "#"
                                + Base64.getEncoder().encodeToString(sessionKey)
                                + "#"
                                + Base64.getEncoder().encodeToString(publicKey.getEncoded());

                Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM, ENCRYPTION_PROVIDER);
                cipher.init(Cipher.ENCRYPT_MODE, bankPubKey);
                byte[] ciphertextBytes = cipher.doFinal(sessionKeyAndSignature.getBytes(StandardCharsets.UTF_8));
                String ciphertext = Base64.getEncoder().encodeToString(ciphertextBytes);

                out.writeObject(ciphertext);
                out.flush();

                // 3. Get client number request and send it
                Object inObj = decryptStringWithAES((byte[]) inputStream.readObject(), sessionKey);

                if (!"CLIENT_NUMBER_REQUEST".equals(inObj)) {
                    LOGGER.error("Authorization failed: " + inObj);
                }

                out.writeObject(encryptStringWithAES(clientNumber, sessionKey));
                out.flush();

                // 4. Get bank's challenge
                String challenge = decryptStringWithAES((byte[]) inputStream.readObject(), sessionKey);
                byte[] decodedChallenge = Base64.getDecoder().decode(challenge);

                //TODO grab user's PIN

                // 5. Sign the challenge, sign it and add the PIN
                byte[] signedChallenge = signMessage(decodedChallenge, privateKey);
                String signedChallengeAndPin =
                        Base64.getEncoder().encodeToString(signedChallenge)
                                + "#"
                                + pin;

                out.writeObject(encryptStringWithAES(signedChallengeAndPin, sessionKey));
                out.flush();

                // 6. Verify bank's response
                inObj = decryptStringWithAES((byte[]) inputStream.readObject(), sessionKey);

                if (!"OK".equals(inObj)) {
                    LOGGER.error("Authorization failed: " + inObj);
                }

                LOGGER.error("Authorization granted");
            } catch (NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                     NoSuchProviderException | InvalidKeyException | ClassNotFoundException e) {
                LOGGER.error(e.getMessage());
            }

        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static PublicKey getBankPubKey() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        String absolutePath = Client.class.getClassLoader().getResource("certificates/keystore.jks").getPath();
        System.out.println(absolutePath);
        FileInputStream fis = new FileInputStream(absolutePath);
        keyStore.load(fis, CERT_PASSWD.toCharArray());
        Certificate bankCert = keyStore.getCertificate("beid");
        return bankCert.getPublicKey();
    }

    private static byte[] signMessage(byte[] message, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA384withECDSA");
            signature.initSign(privateKey);
            signature.update(message);
            return signature.sign();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
