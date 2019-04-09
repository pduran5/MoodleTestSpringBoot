package net.pduran.moodletest.controller;

import net.pduran.moodletest.Pregunta;
import net.pduran.moodletest.storage.StorageFileNotFoundException;
import net.pduran.moodletest.storage.StorageService;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Controller
public class MoodleTestController {
    private final StorageService storageService;
    private String filenameoriginal = "";
    private String testfilename = "";

    @Autowired
    public MoodleTestController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) {
        model.addAttribute("files", storageService.loadAll().map(
                path -> MvcUriComponentsBuilder.fromMethodName(MoodleTestController.class,
                        "serveFile", path.getFileName().toString()).build().toString())
                .collect(Collectors.toList()));

        model.addAttribute("step", "1");
        model.addAttribute("filenameoriginal", filenameoriginal);
        return "moodletest";
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes, Model model) {
        //storageService.deleteAll();
        storageService.store(file);
        filenameoriginal = file.getOriginalFilename();
        model.addAttribute("step", "2");
        return "redirect:/2";
    }

    @GetMapping("/2")
    public String cargar(Model model) {
        model.addAttribute("step", "2");
        return "moodletest";
    }

    @PostMapping("/3")
    public String generar(@RequestParam("centro") String centro,
                          @RequestParam("profesor") String profesor,
                          @RequestParam("ciclonivel") String ciclonivel,
                          @RequestParam("ciclonombre") String ciclonombre,
                          @RequestParam("modulo") String modulo,
                          @RequestParam("uf") String uf,
                          @RequestParam("prueba") String prueba,
                          @RequestParam("fecha") String fecha,
                          @RequestParam("resta") String resta,
                          Model model) {
        testfilename = unZipIt("./upload/" + filenameoriginal, "./upload");

        List lines = readFileInList("./upload/" + testfilename);

        ArrayList enunciado = new ArrayList<String>();
        ArrayList respuestas = new ArrayList<String>();
        ArrayList salidagift = new ArrayList<String>();
        ArrayList salidaweb = new ArrayList<String>();
        ArrayList salidawebsol = new ArrayList<String>();

        int numeropregunta = 1;
        
        StringBuilder sb = new StringBuilder("<html><head>");
        sb.append("<link rel='stylesheet' href='https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css' crossorigin='anonymous'>");
        sb.append("</head><body><div class='container'><table><tbody><tr><td>");
        
        if(centro.equals("Institut Provençana")) {
            sb.append("<img width='120px' src='https://pbs.twimg.com/profile_images/934037138160668678/Pru1g-ci_400x400.jpg'/>");
        }

        sb.append("</td><td><b>");
        sb.append("<p style='margin: 0'>").append(ciclonivel).append(" ").append(ciclonombre).append(".</p>");
        sb.append("<p style='margin: 0'>").append(modulo).append("</p>");
        sb.append("<p style='margin: 0'>Professor: ").append(profesor).append("</p>");
        sb.append("</b></td><td style='width:10%'></td><td><b>");
        sb.append("<p style='margin: 0'>").append(uf).append("</p>");
        sb.append("<p style='margin: 0'>").append(prueba).append("</p>");
        sb.append("<p style='margin: 0'>Data: ").append(fecha).append("</p>");
        sb.append("</b></td></tr></tbody></table>");

        salidaweb.add(sb);
        salidawebsol.add(sb);

        ArrayList<Pregunta> preguntas = new ArrayList<>();

        // Splittear preguntas según delimitador ------
        int idx = 0;
        Iterator<String> itrn = lines.iterator();
        Pregunta p = new Pregunta();
        p.numero = idx + 1;

        while (itrn.hasNext()) {
            String line = itrn.next();
            if (!line.startsWith("-----")) {
                p.linea.add(Sanitize(line));
            } else {
                preguntas.add(p);
                p = new Pregunta();
                idx++;
                p.numero = idx + 1;
            }
        }

        boolean fillintheblanks = false;
        boolean matching = false;

        for (Pregunta pregunta : preguntas) {
            // ENUNCIADO
            for (String line : pregunta.linea) {
                if (line.contains("_____[")) fillintheblanks = true;
                if (line.startsWith("*") && line.contains("->")) matching = true;
            }

            if (!fillintheblanks) {
                salidagift.add("[html]<pre style=\"font-family: PT Serif\">");
            }

            int correct = 0, incorrect = 0;

            for (String linea : pregunta.linea) {
                if (linea.contains("_____")) {
                    int inicio = linea.indexOf("_____[") + 6;
                    int finall = inicio + linea.substring(inicio).indexOf("]");

                    String opciones = linea.substring(inicio, finall);
                    StringBuilder output = new StringBuilder(linea.substring(0, inicio - 6));

                    output.append("{");
                    String[] split = opciones.split("#");

                    for (int j = 0; j < split.length; j++) {
                        output.append("=").append(split[j]);
                        if (j != split.length - 1) output.append(" ");
                    }

                    output.append("}");
                    output.append(linea.substring(finall + 1));
                    salidagift.add(output.toString());
                    salidaweb.add(output.toString());
                    salidawebsol.add(output.toString());
                }
                // Añadir imagen según fichero. Formato ###fichero.png
                else if (linea.startsWith("###")) {
                    File file = new File("./upload/" + linea.substring(3));
                    byte[] bytes = loadFile(file);
                    byte[] encoded = Base64.encodeBase64(bytes);
                    String image = new String(encoded);

                    salidagift.add("<img src=\"data:image/png;base64," + image + "\">");
                    salidaweb.add("<img src=\"data:image/png;base64," + image + "\">");
                    salidawebsol.add("<img src=\"data:image/png;base64," + image + "\">");
                } else if (linea.startsWith("*")) {
                    String[] splitted = linea.substring(1).split("->");
                    String output = "    =" + splitted[0] + " -> " + splitted[1];
                    respuestas.add(output);
                }
                // Guardar respuestas correctas en array
                else if (linea.startsWith("+")) {
                    correct++;
                    respuestas.add(linea);
                } else if (linea.startsWith("-")) {
                    incorrect++;
                    respuestas.add(linea);
                }
                // Añadir enunciado
                else {
                    salidagift.add(linea.length() == 0 ? "<br>" : linea);
                    String ohtml = "<div class='alert alert-primary' role='alert'><h7>";
                    ohtml += "<strong>" + numeropregunta + ".</strong> <i>";
                    numeropregunta++;
                    ohtml += linea.length() == 0 ? "<br>" : linea;
                    ohtml += "</i></h7></div>";
                    salidaweb.add(ohtml);
                    salidawebsol.add(ohtml);
                }
            }

            if (!fillintheblanks) {
                salidagift.add("</pre>\n{");
            }

            // RESPUESTAS

            char letra = 'a';

            salidaweb.add("<ul style='list-style-type: none'>");
            salidawebsol.add("<ul style='list-style-type: none'>");

            for (Object line : respuestas) {
                // Respuesta incorrecta
                if (line.toString().startsWith("-")) {
                    salidagift.add(FormatearRespuesta(incorrect, line, false, resta));
                    salidawebsol.add("<li><strong>" + letra + ")</strong> " + line.toString().substring(1) + "</li>");
                }
                // Respuesta correcta
                else if (line.toString().startsWith("+")) {
                    salidagift.add(FormatearRespuesta(correct, line, true, "0"));
                    salidawebsol.add("<li><strong>" + letra + ") " + line.toString().substring(1) + "</strong></li>");
                }
                // Matching
                else {
                    salidagift.add(line.toString());
                    salidawebsol.add("<li><strong>" + letra + ")</strong> " + line.toString().substring(1) + "</li>");
                }

                salidaweb.add("<li><strong>" + letra + ")</strong> " + line.toString().substring(1) + "</li>");
                letra++;
            }
            salidaweb.add("</ul>");
            salidawebsol.add("</ul>");

            if (!fillintheblanks) {
                salidagift.add("}");
            }

            salidagift.add("\n");
            salidaweb.add("<br>");
            salidawebsol.add("<br>");

            enunciado.clear();
            respuestas.clear();

            fillintheblanks = false;
            matching = false;

        }

        try (PrintWriter outweb = new PrintWriter("./upload/test.html");
             PrintWriter outwebsol = new PrintWriter("./upload/testsol.html");
             PrintWriter outgift = new PrintWriter("./upload/test.gift")) {
            salidaweb.forEach((linea) -> {
                outweb.println(linea);
            });
            salidawebsol.forEach((linea) -> {
                outwebsol.println(linea);
            });
            salidagift.forEach((linea) -> {
                outgift.println(linea);
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        /*
        String[] cmdtest = {"/bin/sh", "-c", "pandoc /home/peter/moodletest/upload/test.html -o /home/peter/moodletest/upload/test.odt"};
        String[] cmdtestsol = {"/bin/sh", "-c", "pandoc /home/peter/moodletest/upload/testsol.html -o /home/peter/moodletest/upload/testsol.odt"};
        try {
            Runtime.getRuntime().exec(cmdtest);
            Runtime.getRuntime().exec(cmdtestsol);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

        return "redirect:/3";
    }

    private static byte[] loadFile(File file) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        long length = file.length();
        byte[] bytes = new byte[(int) length];

        int offset = 0;
        int numRead = 0;
        while (true) {
            try {
                if (!(offset < bytes.length
                        && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            offset += numRead;
        }

        if (offset < bytes.length) {
            try {
                throw new IOException("Could not completely read file " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    private String FormatearRespuesta(int numero, Object line, boolean correcta, String resta) {
        String porcentaje = String.format("%.5f", 100.0 / numero);
        //porcentaje = porcentaje.replace(',', '.');
        //porcentaje = porcentaje.replace(".00000", "");
        String linea = "    ";

        // Respuestas correctas
        if (correcta) {
            if (numero == 1) linea += "=" + line.toString().substring(1);
            else linea += "~%" + porcentaje + "%" + line.toString().substring(1);
        }
        // Respuestas incorrectas
        else {
            switch(resta) {
                // No restar
                case "0": linea += "~" + line.toString().substring(1); break;

                // -0,25
                case "1": linea += "~%-25%" + line.toString().substring(1); break;

                // -0,33
                case "2": linea += "~%-33.33333%" + line.toString().substring(1); break;

                // -0,5
                case "3": linea += "~%-50%" + line.toString().substring(1); break;

                // Penalización proporcional
                case "4": linea += "~%-" + porcentaje + "%" + line.toString().substring(1); break;
            }
        }

        return linea;
    }

    private String Sanitize(String linea) {
        linea = linea.replace("{", "\\{");
        linea = linea.replace("}", "\\}");
        return linea;
    }

    @GetMapping("/3")
    public String cargar3(Model model) {
        model.addAttribute("step", "3");
        return "moodletest";
    }

    @GetMapping("/upload/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

    private String unZipIt(String zipFile, String outputFolder) {
        byte[] buffer = new byte[1024];

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            //create output directory is not exists
            File folder = new File("./upload");
            if (!folder.exists()) {
                folder.mkdir();
            }

            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);

                if (fileName.endsWith(".txt")) {
                    testfilename = fileName;
                }

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return testfilename;
    }

    private static List<String> readFileInList(String fileName) {
        List<String> lines = Collections.emptyList();

        try {
            lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines;
    }

}
