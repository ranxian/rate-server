package com.rate.server;

import com.rate.engine.algorithm.Algorithm;
import com.rate.engine.benchmark.Benchmark;
import com.rate.engine.exception.InvalidArgumentException;
import com.rate.engine.task.Task;
import com.rate.engine.view.View;
import com.rate.utils.*;
import com.rate.utils.StringUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by Ran Xian on 4/1/14.
 */
public class Server {
    public int port = -1;

    public static void main(String args[]) {
        int port = RateConfig.getServerPort();
        System.out.println("Starting rate server at port: " + port);
        Server server = new Server(port);
        server.start();
    }

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(this.port);
            while (true) {
                Socket client = serverSocket.accept();
                new HandlerThread(client);
            }
        } catch (IOException e) {
            System.out.println("Unable to start rate server");
            e.printStackTrace();
        }
    }

    private class HandlerThread implements Runnable {
        private Socket socket;
        private BufferedInputStream input;
        private PrintWriter output;
        private String command;

        public HandlerThread(Socket socket) {
            this.socket = socket;
            new Thread(this).start();
        }

        public void run() {
            try {
                input = new BufferedInputStream(socket.getInputStream());
                output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

                logger.info("Begin talk with " + socket.getInetAddress() + ": " + socket.getPort());

                this.handle();
                input.close();
                output.close();

                logger.info("End talk with " + socket.getInetAddress() + ": " + socket.getPort());
            } catch (Exception e) {
                logger.warn("Server can't talk with client! " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        socket = null;
                        logger.error("Server can't close socket! " + e.getMessage());
                    }
                }
            }
        }

        public void killCommand(HashMap<String, String> args) {
            String uuid = args.get("uuid");
            if (uuid == null) {
                failed("Task uuid must be given");
                return;
            }
            Task task = Task.find(uuid);
            if (task == null) {
                failed("No task with uuid [" + uuid + "]");
                return;
            }

            try {
                task.killSelf();
            } catch (Exception e) {
                failed(e.getMessage());
            }
            success("Task killed");
        }

        private void updateCommand(String target, HashMap<String, String> args) throws Exception {
            String uuid = args.get("uuid");
            if (uuid == null)
                throw new InvalidArgumentException("uuid must be given");
            args.remove("uuid");
            List<String> setCommands = new ArrayList<String>();
            for (String key : args.keySet()) {
                String value = args.get(key);
                setCommands.add(key + "='" + value + "'");
            }
            DBUtils.executeSQL("UPDATE " + target + " SET " + org.apache.commons.lang3.StringUtils.join(setCommands, ",")
                                    + " WHERE uuid=?", uuid);
            success("update success");
        }

        private void failed(String msg) {
            JSONObject object = new JSONObject();
            object.put("result", "failed");
            object.put("command", command);
            if (msg != null)
                object.put("message", msg);
            logger.debug(object.toString());
            println(object.toString());
        }

        private void success() { success(null); }

        private void success(String message) {
            JSONObject object = new JSONObject();
            object.put("result", "success");

            if (message != null) {
                object.put("message", message);
            }

            println(object.toString());
        }

        private void handle() throws Exception {
            while (true) {
                command = StringUtils.readline(input);
                if (command == null)
                    return;

                if (command.equalsIgnoreCase("GOODBYE")) {
                    break;
                }

                String[] sp = command.split(" ");
                HashMap<String, String> parsedArgs = StringUtils.parseArgs(sp);
                logger.info("Recv commands: " + command);

                println("BEGIN");

                try {
                    if (sp[0].equalsIgnoreCase("status")) {
                        status();
                    } else if (sp[0].equalsIgnoreCase("quit")) {
                        break;
                    } else if (sp[0].equalsIgnoreCase("help")) {
                        printHelp();
                    } else if (sp[0].equalsIgnoreCase("list")) {
                        listCommand(sp[1], parsedArgs);
                    } else if (sp[0].equalsIgnoreCase("create")) {
                        createCommand(sp[1], parsedArgs);
                    } else if (sp[0].equalsIgnoreCase("delete")) {
                        deleteCommand(sp[1], parsedArgs);
                    } else if (sp[0].equalsIgnoreCase("update")) {
                        updateCommand(sp[1], parsedArgs);
                    } else if (sp[0].equalsIgnoreCase("info")) {
                        infoCommand(sp[1], parsedArgs);
                    } else if (sp[0].equalsIgnoreCase("run")) {
                        runCommand(parsedArgs);
                    } else if (sp[0].equalsIgnoreCase("kill")) {
                        killCommand(parsedArgs);
                    } else if (sp[0].equalsIgnoreCase("download")) {
                        downloadCommand(sp[1], parsedArgs);
                    } else {
                        failed(null);
                    }
                } catch (IndexOutOfBoundsException e) {
                    failed("not enough arguments");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (!sp[0].equalsIgnoreCase("download"))
                    println("END");
            }
        }

        public Logger logger = Logger.getLogger("CLI");

        public void println(String s) {
            output.println(s);
            output.flush();
        }

        public void print(String s) {
            output.print(s);
            output.flush();
        }

        public void notice(String s) {
            output.println("NOTICE: " + s);
            output.flush();
        }

        public void status() throws Exception {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("result", "success");
            jsonObject.put("view_count", DBUtils.countTable("view"));
            jsonObject.put("sample_count", DBUtils.countTable("sample"));
            jsonObject.put("benchmark_count", DBUtils.countTable("benchmark"));
            jsonObject.put("algorithm_count", DBUtils.countTable("algorithm"));
            jsonObject.put("class_count", DBUtils.countTable("class"));
            println(jsonObject.toString());
        }

        public void printHelp() {
            println("Usage: ");
            println("status:\tshow database status");
            println("quit:\tquit the RATE CLI");
        }

        // List target, target must be in [view, benchmark, algorithm, task]
        public void listCommand(String target, HashMap<String, String> args) throws Exception {
            String sql = "SELECT * FROM " + target;
            sql += " ORDER BY created DESC";
            logger.info("Executing sql: " + sql);

            JSONArray jsonArray = new JSONArray();
            if (target.equals("view")) {
                List<View> views = DBUtils.executeSQL(View.listHandler, sql);
                for (View view : views) {
                    jsonArray.add(view.toJSON());
                }
            } else if (target.equals("benchmark")) {
                List<Benchmark> benchmarks = DBUtils.executeSQL(Benchmark.listHandler, sql);

                for (Benchmark benchmark : benchmarks)
                    jsonArray.add(benchmark.toJSON());
            } else if (target.equals("algorithm")) {
                List<Algorithm> algorithms = DBUtils.executeSQL(Algorithm.listHandler, sql);
                for (Algorithm algorithm : algorithms)
                    jsonArray.add(algorithm.toJSON());
            } else if (target.equals("task")) {
                List<Task> tasks = DBUtils.executeSQL(Task.listHandler, sql);
                for (Task task : tasks) {
                    jsonArray.add(task.toJSON());
                }
            } else {
                failed("can't list " + target);
                return;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("result", "success");
            jsonObject.put("count", jsonArray.size());
            jsonObject.put("contents", jsonArray.toString());
            println(jsonObject.toString());
        }

        public void createCommand(String target, HashMap<String, String> args) {
            logger.info("creating " + target + " using args: " + args.toString());

            JSONObject jsonObject = new JSONObject();
            try {
                if (target.equalsIgnoreCase("view")) {
                    View view;
                    if (args.get("strategy").equals("file")) {
                        File tempFile = new File(UUID.randomUUID().toString() + ".txt");
                        receiveFile(tempFile);
                        args.put("filePath", tempFile.getAbsolutePath());
                        view = ViewUtils.create(args, output);
                        FileUtils.forceDelete(tempFile);
                    } else {
                        view = ViewUtils.create(args, output);
                    }
                    jsonObject = view.toJSON();
                } else if (target.equalsIgnoreCase("benchmark")) {
                    Benchmark benchmark = null;
                    if (args.get("strategy") != null && args.get("strategy").equals("file")) {
                        File tempFile = new File(UUID.randomUUID().toString() + ".txt");
                        System.out.println("here");
                        receiveFile(tempFile);
                        args.put("filePath", tempFile.getAbsolutePath());
                        benchmark = BenchmarkUtils.create(args, output);
                        FileUtils.forceDelete(tempFile);
                    } else {
                        benchmark = BenchmarkUtils.create(args, output);
                    }

                    jsonObject = benchmark.toJSON();
                } else if (target.equalsIgnoreCase("algorithm")) {
                    File tempFile = new File(UUID.randomUUID().toString() + ".zip");
                    receiveFile(tempFile);
                    try {
                        args.put("filePath", tempFile.getAbsolutePath());
                        Algorithm algorithm = AlgorithmUtils.create(args);
                        jsonObject = algorithm.toJSON();
                    } finally {
                        FileUtils.forceDelete(tempFile);
                    }
                } else {
                    failed("can't use " + target + " as target");
                    return;
                }
                jsonObject.put("result", "success");
                println(jsonObject.toString());
            } catch (InvalidArgumentException e) {
              System.out.println(e.getMessage());
                failed(e.getMessage());
            } catch (Exception e) {
                failed(e.getMessage());
                e.printStackTrace();
            }
        }

        public void deleteCommand(String target, HashMap<String, String> args) {
            String uuid = args.get("uuid");

            if (uuid == null) {
                failed(target + " uuid must be given");
                return;
            }

            Boolean deleted = false;
            try {
                if (target.equalsIgnoreCase("view")) {
                    deleted = ViewUtils.delete(uuid);
                } else if (target.equalsIgnoreCase("benchmark")) {
                    deleted = BenchmarkUtils.delete(uuid);
                } else if (target.equalsIgnoreCase("algorithm")) {
                    deleted = AlgorithmUtils.delete(uuid);
                } else if (target.equalsIgnoreCase("task")) {
                    deleted = RunnerUtils.delete(uuid);
                }
            } catch (Exception e) {
                e.printStackTrace();
                failed(e.getMessage());
                return;
            }

            if (!deleted) {
                failed("can't delete " + target + " uuid");
            } else {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("result", "success");
                println(jsonObject.toString());
            }
        }

        public void infoCommand(String target, HashMap<String, String> args) {
            String uuid = args.get("uuid");

            if (uuid == null) {
                failed(target + " uuid must be given");
                return;
            }

            JSONObject jsonObject = new JSONObject();

            try {
                if (target.equalsIgnoreCase("task")) {
                    String info = RunnerUtils.info(args);
                    Task task = Task.find(uuid);
                    jsonObject = task.toJSON();
                    jsonObject.put("task_info", info);
                    if (task.getFinished() != null) {
                        jsonObject.put("done", 1);
                        jsonObject.put("finished", task.getFinished().toString());
                    } else {
                        jsonObject.put("done", 0);
                    }
                } else if (target.equals("view")) {
                    View view = View.find(uuid);
                    if (view == null) {
                        failed("view " + uuid + " not found.");
                        return;
                    } else {
                        jsonObject = view.toJSON();
                    }
                } else if (target.equals("benchmark")) {
                    Benchmark benchmark = Benchmark.find(uuid);
                    if (benchmark == null) {
                        failed("benchmark " + uuid + " not found.");
                        return;
                    } else {
                        jsonObject = benchmark.toJSON();
                    }
                } else if (target.equals("algorithm")) {
                    Algorithm algorithm = Algorithm.find(uuid);
                    if (algorithm == null) {
                        failed("algorithm " + uuid + " not found.");
                        return;
                    } else {
                        jsonObject = algorithm.toJSON();
                    }
                }
            } catch (Exception e) {
                failed(e.getMessage());
                return;
            }

            jsonObject.put("result", "success");
            println(jsonObject.toString());
        }

        public void runCommand(HashMap<String, String> args) throws Exception {
            String uuid = RunnerUtils.run(args);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("result", "success");
            jsonObject.put("uuid", uuid);
            println(jsonObject.toString());
        }

        public void downloadCommand(String target, HashMap<String, String> args) throws Exception {
            File toDownload = null;
            File tempDir = null;

            if (target.equals("task")) {
                String uuid = args.get("uuid");
                Task task = Task.find(uuid);
                File taskDir = new File(task.getDirPath());
                tempDir = new File(RateConfig.getTempRootDir() + "/task-" + uuid);
                if (!tempDir.mkdir())
                    throw new IOException("Can't create temp dir");

                for (File childFile : taskDir.listFiles()) {
                    if (childFile.getName().contains("task.bm") || childFile.getName().contains("task.ongoing")
                            || childFile.getName().contains("state.txt"))
                        continue;
                    if (childFile.isDirectory())
                        FileUtils.copyDirectoryToDirectory(childFile, tempDir);
                    else if (childFile.isFile())
                        FileUtils.copyFileToDirectory(childFile, tempDir);
                }

                toDownload = tempDir;
            } else if (target.equals("benchmark")) {
                String uuid = args.get("uuid");
                Benchmark benchmark = Benchmark.find(uuid);
                toDownload = new File(benchmark.dirPath());
            } else if (target.equals("algorithm")) {
                String uuid = args.get("uuid");
                Algorithm algorithm = Algorithm.find(uuid);
                toDownload = new File(algorithm.dirPath());
            } else if (target.equals("image")) {
                String uuid = args.get("uuid");
                if (uuid == null) throw new InvalidArgumentException("benchmark uuid must be given");
                Benchmark benchmark = Benchmark.find(uuid);
                if (benchmark == null) throw new InvalidArgumentException("benchmark " + uuid + " not found");
                tempDir = new File(RateConfig.getTempRootDir() + "/benchmark-" + uuid);
                if (!tempDir.mkdir())
                    throw new IOException("Can't create temp dir");
                output.println("PROGRESS");
                output.flush();
                List<String> enrolledSamplePaths = benchmark.enrolledSamplePaths();
                int total = enrolledSamplePaths.size();
                int count = 0;

                for (String path : enrolledSamplePaths) {
                    path = RateConfig.getSampleRootDir() + "/" + path;
                    File img = new File(path);
                    FileUtils.copyFileToDirectory(img, tempDir);
                    if (count % 5 == 0) {
                        output.println((double)count/total);
                        output.flush();
                    }
                    count += 1;
                }
                output.println("DONE");
                output.flush();
                toDownload = tempDir;
            } else if (target.equals("view")) {
                String uuid = args.get("uuid");
                if (uuid == null) throw new InvalidArgumentException("view uuid must be given");
                View view = View.find(uuid);
                if (view == null) throw new InvalidArgumentException("view " + uuid + " not found");
                tempDir = new File(RateConfig.getTempRootDir() + "/" + "view-" + uuid);
                if (!tempDir.mkdir())
                    throw new IOException("Can't create temp dir");

                File tempFile = new File(tempDir.getAbsolutePath() + "/view.txt");
                PrintWriter viewWriter = new PrintWriter(tempFile);
                viewWriter.println(view.getExportString());
                viewWriter.close();

                toDownload = tempDir;
            } else {
                failed("download " + target + " not implemented");
            }

            success();
            println("END");
            logger.debug("BEGIN send file");

            SRFileUtils.uploadFiles(toDownload, socket);

            logger.debug("Delete temp dir");
            if (tempDir != null)
                FileUtils.deleteDirectory(tempDir);
        }

        private void receiveFile(File file) throws IOException {
            int length = (int)Long.parseLong(StringUtils.readline(input));
            SRFileUtils.reiceiveFile(file, input, length);
        }
    }
}
