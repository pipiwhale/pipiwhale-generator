package com.poke.maker.meta;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.poke.maker.meta.enums.FileGenerateTypeEnum;
import com.poke.maker.meta.enums.FileTypeEnum;
import com.poke.maker.meta.enums.ModelTypeEnum;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 元信息校验
 */
public class MetaValidator {

    public static void doValidAndFill(Meta meta) {
        validAndFillMetaRoot(meta);
        validAndFillFileConfig(meta);
        validAndFillModelConfig(meta);
    }

    public static void validAndFillModelConfig(Meta meta) {
        Meta.ModelConfig modelConfig = meta.getModelConfig();
        if (modelConfig == null) {
            return;
        }
        // modelConfig 默认值
        List<Meta.ModelConfig.ModelInfo> modelInfoList = modelConfig.getModels();
        if (!CollectionUtil.isNotEmpty(modelInfoList)) {
            return;
        }
        for (Meta.ModelConfig.ModelInfo modelInfo : modelInfoList) {
            // 为 group 类型，不校验
            String groupKey = modelInfo.getGroupKey();
            if (StrUtil.isNotBlank(groupKey)) {
                // 生成中间参数
                String allArgsStr = modelInfo.getModels().stream()
                        .map(subModelInfo -> String.format("\"--%s\"", subModelInfo.getFieldName()))
                        .collect(Collectors.joining(", "));
                modelInfo.setAllArgsStr(allArgsStr);

                for (Meta.ModelConfig.ModelInfo subModelInfo : modelInfo.getModels()) {
                    validateModelInfo(subModelInfo);
                }
                continue;
            }
            validateModelInfo(modelInfo);
        }
    }

    private static void validateModelInfo(Meta.ModelConfig.ModelInfo modelInfo) {
        // 输出路径默认值
        String fieldName = modelInfo.getFieldName();
        if (StrUtil.isBlank(fieldName)) {
            throw new MetaException("未填写 fieldName");
        }

        // 模型配置
        String modelInfoType = modelInfo.getType();
        if (StrUtil.isEmpty(modelInfoType)) {
            modelInfo.setType(ModelTypeEnum.STRING.getValue());
        }
    }

    public static void validAndFillFileConfig(Meta meta) {
        // fileConfig 默认值
        Meta.FileConfig fileConfig = meta.getFileConfig();
        if (fileConfig == null) {
            return;
        }
        // sourceRootPath：必填
        String sourceRootPath = fileConfig.getSourceRootPath();
        if (StrUtil.isBlank(sourceRootPath)) {
            throw new MetaException("未填写 sourceRootPath");
        }
        // inputRootPath：.source + sourceRootPath 的最后一个层级路径
        String inputRootPath = fileConfig.getInputRootPath();
        String defaultInputRootPath = ".source/" + FileUtil.getLastPathEle(Paths.get(sourceRootPath)).getFileName().toString();
        if (StrUtil.isEmpty(inputRootPath)) {
            fileConfig.setInputRootPath(defaultInputRootPath);
        }
        // outputRootPath：默认为当前路径下的 generated
        String outputRootPath = fileConfig.getOutputRootPath();
        String defaultOutputRootPath = "generated";
        if (StrUtil.isEmpty(outputRootPath)) {
            fileConfig.setOutputRootPath(defaultOutputRootPath);
        }
        String fileConfigType = fileConfig.getType();
        String defaultType = FileTypeEnum.DIR.getValue();
        if (StrUtil.isEmpty(fileConfigType)) {
            fileConfig.setType(defaultType);
        }

        // fileInfo 默认值
        List<Meta.FileConfig.FileInfo> fileInfoList = fileConfig.getFiles();
        if (!CollectionUtil.isNotEmpty(fileInfoList)) {
            return;
        }
        for (Meta.FileConfig.FileInfo fileInfo : fileInfoList) {
            String type = fileInfo.getType();
            // 类型为 group，不校验
            if (FileTypeEnum.GROUP.getValue().equals(type)) {
                for (Meta.FileConfig.FileInfo subFileInfo : fileInfo.getFiles()) {
                    validateFileInfo(subFileInfo);
                }
                continue;
            }
            validateFileInfo(fileInfo);
        }
    }

    private static void validateFileInfo(Meta.FileConfig.FileInfo fileInfo) {
        // inputPath: 必填
        String inputPath = fileInfo.getInputPath();
        if (StrUtil.isBlank(inputPath)) {
            throw new MetaException("未填写 inputPath");
        }

        // type：默认 inputPath 有文件后缀（如 .java）为 file，否则为 dir
        String type = fileInfo.getType();
        if (StrUtil.isBlank(type)) {
            // 无文件后缀
            if (StrUtil.isBlank(FileUtil.getSuffix(inputPath))) {
                fileInfo.setType(FileTypeEnum.DIR.getValue());
            } else {
                fileInfo.setType(FileTypeEnum.FILE.getValue());
            }
        }

        // generateType：如果文件结尾不为 ftl，generateType 默认为 static，否则为 dynamic
        String generateType = fileInfo.getGenerateType();
        if (StrUtil.isBlank(generateType)) {
            // 为动态模板
            if (inputPath.endsWith(".ftl")) {
                generateType = FileGenerateTypeEnum.DYNAMIC.getValue();
            } else {
                generateType = FileGenerateTypeEnum.STATIC.getValue();
            }
            fileInfo.setGenerateType(generateType);
        }

        // outputPath: 静态文件默认等于 inputPath，动态文件默认为 inputPath去掉.ftl
        String outputPath = fileInfo.getOutputPath();
        if (StrUtil.isEmpty(outputPath)) {
            if (generateType.equals(FileGenerateTypeEnum.DYNAMIC.getValue())) {
                inputPath = inputPath.replace(".ftl", "");
            }
            fileInfo.setOutputPath(inputPath);
        }
    }

    public static void validAndFillMetaRoot(Meta meta) {
        // 校验并填充默认值
        String name = StrUtil.blankToDefault(meta.getName(), "my-generator");
        String description = StrUtil.emptyToDefault(meta.getDescription(), "我的模板代码生成器");
        String author = StrUtil.emptyToDefault(meta.getAuthor(), "pipiwhale");
        String basePackage = StrUtil.blankToDefault(meta.getBasePackage(), "com.poke");
        String version = StrUtil.emptyToDefault(meta.getVersion(), "1.0");
        String createTime = StrUtil.emptyToDefault(meta.getCreateTime(), DateUtil.now());
        meta.setName(name);
        meta.setDescription(description);
        meta.setAuthor(author);
        meta.setBasePackage(basePackage);
        meta.setVersion(version);
        meta.setCreateTime(createTime);
    }
}
