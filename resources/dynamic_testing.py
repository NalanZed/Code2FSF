import subprocess
import re
import random
import json
import argparse
import time
from typing import List
from z3 import *

start_time = 0
# Create the Z3 solver
solver = Solver()

RESOURCE_DIR = "resources"
RUNNABLE_DOR= "resources/runnable"

def extract_variables(expr):
    """
    Extract variable names (identifiers starting with a letter) from the logical expression, excluding Z3 keywords.
    """
    z3_keywords = {"And", "Or", "Not", "Implies", "True", "False"}
    all_vars = set(re.findall(r'\b[a-zA-Z]\w*\b', expr))
    return all_vars - z3_keywords

def preprocess_expression(expr):
    """
    Preprocess the logical expression:
    1. Remove meaningless underscores (_)
    2. Replace logical operators
    3. Replace special symbols (e.g., → replaced with Implies)
    4. Replace boolean values (true/false replaced with True/False)
    """
    # Remove meaningless underscores after variable names
    expr = re.sub(r'\b(\w+)_\b', r'\1', expr)

    # Replace boolean values
    expr = expr.replace("true", "True").replace("false", "False")

    # Replace logical operators
    expr = expr.replace("&&", ",").replace("||", ",")
    expr = re.sub(r'!\s*\((.*?)\)', r'Not(\1)', expr)  # !(...) -> Not(...)
    # Replace '=' with '==' to avoid ===
    expr = re.sub(r'(?<![<>=!])=(?!=)', '==', expr)  # Replace single "=" with "=="
    expr = expr.replace("→", ", Implies")  # Replace logical symbols

    # Replace logical operators with function form
    expr = re.sub(r'\bAnd\b', 'And', expr)
    expr = re.sub(r'\bOr\b', 'Or', expr)

    # Remove extra spaces
    expr = re.sub(r'\s+', ' ', expr)
    return expr

def parse_to_z3(user_expr, variables):
    """
    Parse the user input logical expression to a Z3-compatible expression, ensuring logical keywords are not replaced.
    """
    # Replace variable names with Z3 symbolic variable references
    for var in variables:
        user_expr = re.sub(rf'\b{var}\b', f'variables["{var}"]', user_expr)
    return user_expr

def get_class_name(java_code: str):
    match = re.search(r'class\s+(\w+)', java_code)
    if match:
        return match.group(1)  # 返回匹配的类名
    else:
        return None  # 如果没有匹配到，返回 None

def run_java_code(java_code: str) -> str:
    classname = get_class_name(java_code)
    file_path = RUNNABLE_DOR + "/"  + classname + ".java"
    with open(file_path, "w") as file:
        file.write(java_code)
    try:
        subprocess.run(["javac", file_path], check=True)
    except subprocess.CalledProcessError:
        print("Error during Java compilation.")
        return ""

    try:
        result = subprocess.run(
            ["java", file_path],
            capture_output=True,
            text=True,
        )
        # print(" result.stdout:" + result.stdout)
        return result.stdout
    except subprocess.CalledProcessError:
        print("Error during Java execution.")
        return ""

def parse_execution_path(execution_output: str) -> List[str]:
    lines = execution_output.splitlines()
    execution_path = []

    for line in lines:
        if "current value" in line or "Entering loop" in line or "Exiting loop" in line or "Evaluating if condition" in line \
                or "Return statement" in line:
            execution_path.append(line)

    return execution_path

def combind_expr_and_list(expr: str, exprList: List[str]):
    #默认T和preCts中的Ct都是用()包围起来的
    com_expr = expr
    for ct in exprList:
        com_expr = f"{com_expr} && !({ct})"
    return com_expr.strip().strip("&&")

def generate_logical_expression(t, previous_cts):
    """
    Combine T and historical Ct conditions to generate a new logical expression.
    :param t: Test condition T (for example, "x >= 0"). repeat_execution_with_ct
    :param previous_cts: list of historical Ct conditions.
    :return: indicates a new logical expression.
    """
    # The initial logical expression is T
    combined_expression = t

    # Use set weight removal to avoid duplicate Ct conditions
    unique_cts = list(set(previous_cts))

    # Accumulate all Ct conditions and invert them
    for ct in unique_cts:
        combined_expression = f"{combined_expression} && !( {ct} )"

    return combined_expression

def simplify_expression(expression):
    """
    Simplify logical expressions and remove redundant negations and redundant conditions.
    """
    # Remove double negatives!! (expr) -> (expr)
    expression = re.sub(r'!\(!\((.*?)\)\)', r'\1', expression)
    expression = re.sub(r'\s+', ' ', expression)  # Remove excess space
    return expression

def solver_check_z3(logic_expr:str)->str:
    try:
        preprocessed_input = preprocess_expression(logic_expr)
        variables = {}
        variable_names = extract_variables(preprocessed_input)
        for var in variable_names:
            variables[var] = Int(var)

        parsed_expr = parse_to_z3(preprocessed_input, variables)
        z3_expr = eval(parsed_expr, {"variables": variables, "And": And, "Or": Or, "Not": Not, "Implies": Implies})
        solver.add(z3_expr)

        if solver.check() == sat:
            print("表达式是可满足的")
            model = solver.model()
            print("满足条件的解:")
            counter_example = ""
            for v in variables.values():
                print(f"{v} = {model[v]}")
                counter_example = counter_example + f"{v} = {model[v]}" + ","
            return counter_example.strip().strip(",")
        else:
            print("The expression is unsatisfiable")
            #创建 Result 对象
            return "OK"

    except Exception as e:
        print("solver check fail!")
        print("错误信息:", e)
        return "ERROR"

def replace_variables(current_condition: str, variable: str, new_value: str) -> str:
    """
    Replace the variable in the logical condition with the new value.
    """
    pattern = rf'\b{re.escape(variable)}\b'  # Match variable names exactly
    return re.sub(pattern, new_value, current_condition)

class Result:
    def __init__(self, status: int, counter_example: str, path_constrain: str):
        self.status = status
        self.counter_example = counter_example  # string 类型字段
        self.path_constrain = path_constrain
    def to_json(self) -> str:
        """
        将 Result 对象序列化为 JSON 字符串
        """
        return json.dumps(self.__dict__)
    @classmethod
    def from_json(cls, json_string: str) -> 'Result':
        data_dict = json.loads(json_string)
        return cls(data_dict["status"], data_dict["counter_example"], data_dict["path_constrain"])
    def __str__(self):
        return f"Result(status={self.status}, counter_example={self.counter_example}, path_constrain={self.path_constrain})"

class SpecUnit:
    def __init__(self, program: str, T: str, D: str, pre_constrains: List[str]):
        self.program = program  # string 类型字段
        self.T = T
        self.D = D
        self.pre_constrains = pre_constrains

    def to_json(self) -> str:
        """
        将 SpecUnit 对象序列化为 JSON 字符串
        """
        return json.dumps(self.__dict__)

    @classmethod
    def from_json(cls, json_string: str) -> 'SpecUnit':
        """
        从 JSON 字符串反序列化为 SpecUnit 对象
        """
        data_dict = json.loads(json_string)
        return cls(data_dict["program"], data_dict["T"], data_dict["D"],data_dict["pre_constrains"])

    def __str__(self):
        return f"SpecUnit(name={self.program}, T={self.T}, D={self.D},pre_constrains={self.pre_constrains})"

def generate_test_spec_unit():
    program = read_java_code_from_file(RESOURCE_DIR + "/" + "TestCase.java")
    T = "num < 0"
    D = "result == -num"
    pre_constrains = {}
    su = SpecUnit(program, T, D,pre_constrains)
    return su.to_json()

def deal_with_spec_unit_json(spec_unit_json: str):
    #读取SpecUnit对象
    spec_unit = None
    # print(f"Processing SpecUnit JSON: {spec_unit_json}")
    try:
        spec_unit = SpecUnit.from_json(spec_unit_json)
    except json.JSONDecodeError as e:
        print(f"Error decoding JSON: {e}")
    program = spec_unit.program
    T = spec_unit.T
    D = spec_unit.D
    previous_cts = spec_unit.pre_constrains

    #运行程序,获得路径输出
    execution_output = run_java_code(program)
    if not execution_output:
        print("No output from Java code execution.")
    #分析路径输出，得到本次执行路径相关的Ct
    execution_path = parse_execution_path(execution_output)
    print("\nExecution Path:")
    for step in execution_path:
        print(step)
    print("end Execution Path")
    current_ct = get_ct_from_execution_path(execution_path);
    if(current_ct == ""):
        current_ct = "true"
    print(f"本次路径对应的_Ct_: {current_ct}")
    new_d = update_D_with_execution_path(D,execution_path)
    print("new_d:" + new_d)
    # 构建新的逻辑表达式并检查可满足性
    negated_d = f"!({new_d})"
    new_logic_expression = f"{T} && {current_ct} && {negated_d}"
    new_logic_expression = simplify_expression(new_logic_expression)
    print(f"\nT && Ct && !D: {new_logic_expression}")
    solver_result = solver_check_z3(new_logic_expression)
    result = ""
    if solver_result == "OK":
        #组装 combined_expr
        previous_cts.append(current_ct)
        combined_expr = combind_expr_and_list(f"({T})", previous_cts)
        print("完成一轮路径验证后，当前(T) && !(previous_cts) && !(current_ct): " + combined_expr)
        scr = solver_check_z3(combined_expr)
        if(scr == "OK"):
            result = Result(3,"",current_ct)
        elif(scr == "ERROR"):
            result = Result(1,scr,"")
        else:
            result = Result(0,"",current_ct)
        print("result:" + result.to_json())
    elif solver_result == "ERROR":
        result = Result(1,"",current_ct)
    else:
        result = Result(2,solver_result,"")
    # print("result:" + result.to_json())

def get_ct_from_execution_path(execution_path:List[str]):
    ct = ""
    for step in reversed (execution_path):
        if "Evaluating if condition" in step:
            condition_match = re.search(r"Evaluating if condition: (.*?) is evaluated as: (.*?)", step)
            if condition_match:
                if_condition = condition_match.group(1).strip()
                ct = f"{ct} && ({if_condition})"
            # Check whether it is a condition to enter the loop
        if "Entering loop" in step:
            condition_match = re.search(r"Entering loop with condition: (.*?) is evaluated as: true", step)
            if condition_match:
                loop_condition = condition_match.group(1).strip()
                ct = f"{ct} && ({loop_condition})"

            # Check whether it is a condition for exiting the loop
        elif "Exiting loop" in step:
            condition_match = re.search(r"Exiting loop, condition no longer holds: (.*?) is evaluated as: false", step)
            if condition_match:
                loop_condition = condition_match.group(1).strip()
                ct = f"{ct} && !({loop_condition})"

            # Check for variable assignment
        elif "current value" in step:
            assignment_match = re.search(r"(.*?) = (.*?), current value of (.*?): (.*?)$", step)
            if assignment_match:
                variable = assignment_match.group(1).strip()
                value = assignment_match.group(2).strip()
                ct = replace_variables(ct,variable,value)

    #先去掉空格，再去掉多余的 &&
    return ct.strip().strip("&&")

def update_D_with_execution_path(D: str, execution_path: List[str]) -> str:
    split_d = D.split("&&")
    update_d = []
    newd = ""
    for step in reversed(execution_path):
        if "current value" in step:
            assignment_match = re.search(r"(.*?) = (.*?), current value of (.*?): (.*?)$", step)
            if assignment_match:
                variable = assignment_match.group(1).strip()
                value = assignment_match.group(2).strip()
                for sd in split_d:
                    if variable in sd:
                        # 替换 D 中的变量
                        sd = replace_variables(sd, variable, value)
                    update_d.append(sd.strip())
                split_d = update_d
                update_d = []
    for ud in split_d:
        newd = f"{newd} && ({ud})"
    return newd.strip().strip("&&")

def read_java_code_from_file(file_path):
    """
    Read Java code from the specified file.
    """
    with open(file_path, "r") as file:
        java_code = file.read()
    return java_code

def test_main():
    init_files();
    spec_unit_json = generate_test_spec_unit()
    print(spec_unit_json)
    if spec_unit_json is not None:
        deal_with_spec_unit_json(spec_unit_json)

def main():
    #创建解析器
    parser = argparse.ArgumentParser()
    # 添加参数定义
    parser.add_argument('-s', '--specUnit', '--specunit', help='输入要验证的SpecUnit对象的JSON字符串', required=False)
    # 解析命令行参数
    args = parser.parse_args()
    spec_unit_json = args.specUnit
    if(spec_unit_json is None):
        print("请提供输入SpecUnit对象的JSON字符串")
        return
    deal_with_spec_unit_json(spec_unit_json)

def test_main_2():
    combined_expr = combind_expr_and_list(" (num>0) ", ["(num > 1)", "(num > 2)"])
    print(combined_expr)

def test_main_3():
    execution_path = ["Evaluating if condition: (b1 == false) is evaluated as: true",
                      "return_value = false , current value of return_value: false"]
    D = "return_value == false"
    r = update_D_with_execution_path(D,execution_path)
    print(r)


def init_files():
    import os
    if not os.path.exists(RESOURCE_DIR):
        os.makedirs(RESOURCE_DIR)
    if not os.path.exists(RUNNABLE_DOR):
        os.makedirs(RUNNABLE_DOR)
    if(not os.path.exists(RESOURCE_DIR + "/TestCase.java")):
        program = """
            public class TestCase{
                public static int Abs(int num){
                    if(num < 0){
                        System.out.println("Evaluating if condition: num < 0 is evaluated as: " + (num < 0));
                        return -num;
                    }
                    else{
                        return num;
                    }
                }
            
                public static void main(String[] args){
                    int num = -3;
                    int result = TestCase.Abs(num);
                    System.out.println("result = Abs.Abs(num), current value of result: " + result);
                }
            }
            """
        with open(RESOURCE_DIR + "/TestCase.java", "w") as file:
            file.write(program)
if __name__ == "__main__":
    # test_main_2()
    # test_main_3()
    # test_main()
    main()

