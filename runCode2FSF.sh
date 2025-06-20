# 读取参数
input=""
maxRounds=3;
model=""
inputDir=""

mvn clean package

while [[ $# -gt 0 ]]; do
  case $1 in
      --maxRounds)
        maxRounds="$2"
        shift 2
        ;;
    --input)
      input="$2"
      shift 2
      ;;
    --model)
      model="$2"
      shift 2
      ;;
    --inputDir)
      inputDir="$2"
      shift 2
      ;;
    *)
      shift
      ;;
  esac
done
# 构建 Java 命令参数
cmd="java -jar target/Code2FSF-1.0.jar"
[[ -n "$input" ]] && cmd="$cmd --input \"$input\""
[[ -n "$model" ]] && cmd="$cmd --model \"$model\""
[[ -n "$inputDir" ]] && cmd="$cmd --inputDir \"$inputDir\""
[[ -n "$maxRounds" ]] && cmd="$cmd --maxRounds $maxRounds"

eval $cmd