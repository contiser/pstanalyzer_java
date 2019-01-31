import libpst.main.java.com.pff.*;

import java.io.IOException;
import java.util.*;

public class PstAnalyzer {
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

            pst.close();

        } catch (IllegalArgumentException | IOException | PSTException e) {
            System.out.println("Error Opening PST");
        }
    }

    private void checkInput(String[] input) {
        if (input.length != 1) {
            throw new IllegalArgumentException();
        }
    }

    private void getFolderMembers(List<PSTFolder> list) {
        for (PSTFolder f : list) {
            System.out.println(f.getDisplayName());
        }
    }

    private void getInboxFolders(PSTFolder folder) throws PSTException, IOException {
        if (folder.hasSubfolders()) {
            for (PSTFolder pstfolder : folder.getSubFolders()) {
                getInboxFolders(pstfolder);
            }
        } else if (!isSentItems(folder)) {
            this.inboxFolders.add(folder);
        }
    }

    private void getSentFolders(PSTFolder folder) throws PSTException, IOException {
        if (folder.hasSubfolders()) {
            for (PSTFolder pstfolder : folder.getSubFolders()) {
                getSentFolders(pstfolder);
            }
        } else if (isSentItems(folder)) {
            this.sentFolders.add(folder);
        }
    }

    private boolean isSentItems(PSTFolder folder) {
        return folder.getDisplayName().toLowerCase().contains("inviata") || folder.getDisplayName().toLowerCase().contains("sent") || folder.getDisplayName().toLowerCase().contains("gesendete");
    }

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

    private void getSentToMe(List<PSTFolder> inboxFolders) throws PSTException, IOException {
        for (PSTFolder f : inboxFolders) {
            getSentToMe(f);
        }
    }

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

    private void getSentFromMe(List<PSTFolder> sentFolders) throws PSTException, IOException {
        for (PSTFolder f : sentFolders) {
            getSentFromMe(f);
        }
    }


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

    private void getAllRecipients(List<PSTFolder> inboxFolders) throws PSTException, IOException {
        for (PSTFolder f : inboxFolders) {
            getAllRecipients(f);
        }
    }

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

    private void getAllSenders(List<PSTFolder> sentFolders) throws PSTException, IOException {
        for (PSTFolder f : sentFolders) {
            getAllSenders(f);
        }
    }

    private void updateList(Map<String, Integer> list, String key) {
        if (list.containsKey(key)) {
            int oldValue = list.get(key);
            list.replace(key, ++oldValue);
        } else {
            list.put(key, 1);
        }
    }

    private void getMaxRecipient(){
        if (recipients.size() > 0) {
            System.out.println("From recipients: ");
            System.out.println(Collections.max(recipients.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey());
        }
    }

    private void getMaxSender(){
        if (senders.size() > 0) {
            System.out.println("From senders: ");
            System.out.println(Collections.max(senders.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey());
        }
    }
}
