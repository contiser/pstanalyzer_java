/*
 * Copyright (c) Sergio Conti Rossini (ch1r0n) 2019. All rights reserved.
 * All rights for any external library to their creators.
 */

import libpst.main.java.com.pff.*;

import java.io.IOException;
import java.util.*;

public class PstAnalyzer {
    // @inboxFolders: List containing all folders where the received messages could be stored (any other than Sent Items).
    // @sentFolders: List containing folders revealed as containing Sent Items from the "isSentItems" method.
    // @recipients: List containing all recipients extracted from the analysed emails.
    // @senders: List containing all senders extracted from analysed emails.

    private List<PSTFolder> inboxFolders = new ArrayList<PSTFolder>();
    private List<PSTFolder> sentFolders = new ArrayList<PSTFolder>();
    private HashMap<String, Integer> recipients = new HashMap<String, Integer>();
    private HashMap<String, Integer> senders = new HashMap<String, Integer>();

    public static void main(String[] args) {
        PstAnalyzer pstAnalysis = new PstAnalyzer();

        try {
            pstAnalysis.checkInput(args);
            PSTFile pst = new PSTFile(args[0]);
            PSTFolder rootFolder = pst.getRootFolder();
            pstAnalysis.getInboxFolders(rootFolder);
            pstAnalysis.getSentFolders(rootFolder);
            pstAnalysis.getFolderMembers(pstAnalysis.inboxFolders);
            pstAnalysis.getFolderMembers(pstAnalysis.sentFolders);
            pstAnalysis.getSentToMe(pstAnalysis.inboxFolders);
            pstAnalysis.getAllRecipients(pstAnalysis.inboxFolders);
            pstAnalysis.getSentFromMe(pstAnalysis.sentFolders);
            pstAnalysis.getAllSenders(pstAnalysis.sentFolders);
            pstAnalysis.getMaxRecipient();
            pstAnalysis.getMaxSender();
            pstAnalysis.printRecipients();
            pstAnalysis.printSenders();

            pst.close();

        } catch (IllegalArgumentException | IOException | PSTException e) {
            System.out.println("Error Opening PST");
        }
    }

    // Method to check if the user provided an argument as requested.

    private void checkInput(String[] input) {
        if (input.length != 1) {
            throw new IllegalArgumentException();
        }
    }

    // Method to get the Display Name of folders contained into the list given as parameter.

    private void getFolderMembers(List<PSTFolder> list) {
        for (PSTFolder f : list) {
            System.out.println(f.getDisplayName());
        }
    }

    // Method to put all the folders not resulting as Sent Items, through the IsSentItems method, into the inboxFolders list.

    private void getInboxFolders(PSTFolder folder) throws PSTException, IOException {
        if (folder.hasSubfolders()) {
            for (PSTFolder pstfolder : folder.getSubFolders()) {
                getInboxFolders(pstfolder);
            }
        } else if (!isSentItems(folder)) {
            this.inboxFolders.add(folder);
        }
    }

    // Method to put all the folders resulting as Sent Items, through the IsSentItems method, into the sentFolders list.

    private void getSentFolders(PSTFolder folder) throws PSTException, IOException {
        if (folder.hasSubfolders()) {
            for (PSTFolder pstfolder : folder.getSubFolders()) {
                getSentFolders(pstfolder);
            }
        } else if (isSentItems(folder)) {
            this.sentFolders.add(folder);
        }
    }

    // Method to determine whether a folder contains Sent Items or could contain received items.

    private boolean isSentItems(PSTFolder folder) {
        return folder.getDisplayName().toLowerCase().contains("inviata") || folder.getDisplayName().toLowerCase().contains("sent") || folder.getDisplayName().toLowerCase().contains("gesendete");
    }

    // Method to find emails sent to the PST File owner, get the recipient field and put it into the recipients List.

    private void getSentToMe(PSTFolder folder) throws PSTException, IOException {
        for (PSTFolder f : folder.getSubFolders()) {
            getSentToMe(f);
        }
        if (!folder.hasSubfolders()) {
            PSTMessage actualMessage = (PSTMessage) folder.getNextChild();
            while (actualMessage != null) {
                if (actualMessage.getMessageToMe()) {
                    for (int counter = 0; counter < actualMessage.getNumberOfRecipients(); counter++) {
                        String recipient = actualMessage.getRecipient(counter).getEmailAddress();
                        updateList(recipients, recipient);
                    }
                }
                actualMessage = (PSTMessage) folder.getNextChild();
            }
        }
    }

    // Overload of the original getSentToMe method to allow lists as input.

    private void getSentToMe(List<PSTFolder> inboxFolders) throws PSTException, IOException {
        for (PSTFolder f : inboxFolders) {
            getSentToMe(f);
        }
    }

    // Method to find emails sent from the PST File owner, get the sender field and put it into the senders List.

    private void getSentFromMe(PSTFolder folder) throws PSTException, IOException {
        for (PSTFolder f : folder.getSubFolders()) {
            getSentToMe(f);
        }
        if (!folder.hasSubfolders()) {
            PSTMessage actualMessage = (PSTMessage) folder.getNextChild();
            while (actualMessage != null) {
                if (actualMessage.isFromMe()) {
                    String as = actualMessage.getSenderEmailAddress();
                    updateList(senders, as);
                }
                actualMessage = (PSTMessage) folder.getNextChild();
            }
        }
    }

    // Overload of the original getSentFromMe method to allow lists as input.

    private void getSentFromMe(List<PSTFolder> sentFolders) throws PSTException, IOException {
        for (PSTFolder f : sentFolders) {
            getSentFromMe(f);
        }
    }

    // Method to get all the recipients of all the messages contained into the folders from the inboxFolders list.

    private void getAllRecipients(PSTFolder folder) throws PSTException, IOException {
        for (PSTFolder f : folder.getSubFolders()) {
            getAllRecipients(f);
        }
        PSTMessage message = (PSTMessage) folder.getNextChild();
        while (message != null) {
            for (int counter = 0; counter < message.getNumberOfRecipients(); counter++) {
                String recipient = message.getRecipient(counter).getEmailAddress();
                updateList(recipients, recipient);
            }
            message = (PSTMessage) folder.getNextChild();
        }

    }

    // Overload of the original getAllRecipients method to allow lists as input.

    private void getAllRecipients(List<PSTFolder> inboxFolders) throws PSTException, IOException {
        for (PSTFolder f : inboxFolders) {
            getAllRecipients(f);
        }
    }

    // Method to get all the senders of all the messages contained into the folders from the sentItems list.

    private void getAllSenders(PSTFolder folder) throws PSTException, IOException {
        for (PSTFolder f : folder.getSubFolders()) {
            getAllSenders(f);
        }
        PSTMessage message = (PSTMessage) folder.getNextChild();
        while (message != null) {
            String sender = message.getSenderEmailAddress();
            updateList(senders, sender);
            message = (PSTMessage) folder.getNextChild();
        }
    }

    // Overload of the getAllSenders original method in order to allow lists as input.

    private void getAllSenders(List<PSTFolder> sentFolders) throws PSTException, IOException {
        for (PSTFolder f : sentFolders) {
            getAllSenders(f);
        }
    }

    // Method to add entries into the lists or increment the count if already present.

    private void updateList(Map<String, Integer> list, String key) {
        if (list.containsKey(key)) {
            int oldValue = list.get(key);
            list.replace(key, ++oldValue);
        } else {
            list.put(key, 1);
        }
    }

    // Method to print the entry with the highest count from the recipients list.

    private void getMaxRecipient() {
        if (recipients.size() > 0) {
            System.out.println("From recipients: ");
            System.out.println(Collections.max(recipients.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey());
        }
    }

    // Method to print the entry with the highest count from the senders list.

    private void getMaxSender() {
        if (senders.size() > 0) {
            System.out.println("From senders: ");
            System.out.println(Collections.max(senders.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey());
        }
    }

    // Method to print all recipients from the recipients list.

    private void printRecipients(){
        System.out.println("All recipients:");
        if(recipients.size() > 0){
            recipients.forEach((sender, count) -> System.out.println(sender + " (" + count + ")"));
        }
    }

    // Method to print all senders from the senders list.

    private void printSenders(){
        System.out.println("All senders:");
        if(senders.size() > 0){
            senders.forEach((sender, count) -> System.out.println(sender + " (" + count + ")"));
        }
    }
}
