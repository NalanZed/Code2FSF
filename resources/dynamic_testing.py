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

def solve_logic_expression(logic_expr):
    """
    Solve the logical expression.
    Parameters: logic_expr (str) - The input logical expression
    Returns: A satisfying solution or unsatisfiable information
    """
    try:
        # Create a new solver instance
        local_solver = Solver()

        # Preprocess the logical expression
        preprocessed_input = preprocess_expression(logic_expr)

        # Extract variables and dynamically create Z3 symbolic variables
        variables = {}
        variable_names = extract_variables(preprocessed_input)
        for var in variable_names:
            variables[var] = Int(var)  # Use Int to handle integers

        # Parse the expression to Z3 format
        parsed_expr = parse_to_z3(preprocessed_input, variables)
        print("Debug: The converted expression is ->", parsed_expr)  # Debug information

        # Use eval to convert to Z3 expression
        z3_expr = eval(parsed_expr)
        local_solver.add(z3_expr)

        # Check satisfiability
        if local_solver.check() == sat:
            print("The expression is satisfiable")
            model = local_solver.model()
            results = {v: model[variables[v]] for v in variables}
            return results
        else:
            return "The expression is unsatisfiable"

    except Exception as e:
        print("The expression is unsatisfiable")
        # print("Error message:", e)
        # return f"Error message: {e}"

def read_java_code_from_file(file_path):
    """
    Read Java code from the specified file.
    """
    with open(file_path, "r") as file:
        java_code = file.read()
    return java_code

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
        return result.stdout
    except subprocess.CalledProcessError:
        print("Error during Java execution.")
        return ""

def parse_execution_path(execution_output: str) -> List[str]:
    lines = execution_output.splitlines()
    execution_path = []

    for line in lines:
        if "current value" in line or "Entering loop" in line or "Exiting loop" in line or "Evaluating if condition" in line:
            execution_path.append(line)

    return execution_path

def combind_T_and_preCt(T: str , preCts: List[str]):
    #默认T和preCts中的Ct都是用()包围起来的
    com_expr = T
    for ct in preCts:
        com_expr = f"{com_expr} && {ct}"
    return com_expr

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

def evaluate_expression(expr, values):
    """
    Evaluates a logical expression using the provided variable values.
    :param expr: The logical expression as a string (e.g., 'x >= 0 && !(x - 1 >= 0)').
    :param values: A dictionary of variable values (e.g., {'x': 5}).
    :return: Boolean result of the expression or an error message.
    """
    try:
        # Step 1: Replace logical operators with Python equivalents
        python_expr = expr.replace("&&", "and").replace("||", "or").replace("!", "not")

        # Step 2: Replace variables with their values in the expression
        for var, value in values.items():
            python_expr = re.sub(rf'\b{var}\b', str(value), python_expr)

        # Debug: Print the transformed expression for verification
        # print(f"Debug: Evaluating Python expression: '{python_expr}'")

        # Step 3: Evaluate the logical expression
        result = eval(python_expr)
        return result
    except Exception as e:
        # Print the error message for debugging
        # print(f"Error during expression evaluation: {e}")
        # print(f"Original Expression: '{expr}'")
        # print(f"Transformed Expression: '{python_expr}'")
        return False

def generate_random_inputs(logical_expression, variables, previous_cts, used_inputs, max_attempts=100):
    """
    Generate satisfying T and! (Ct1) && ! (Ct2) && ...  Random input to ensure that the results are not duplicated.
    :param logical_expression: indicates the test condition T.
    :param variables: List of variables.
    :param previous_cts: list of historical Ct conditions.
    :param used_inputs: Set of used inputs.
    :param max_attempts: indicates the maximum number of attempts.
    :return: The input dictionary that satisfies the condition, or None if the solution is not found.
    """
    # Combine all conditions: T &&! (Ct1) && ! (Ct2) && ...
    combined_condition = logical_expression
    for ct in previous_cts:
        combined_condition = f"{combined_condition} && !( {ct} )"

    # print(f"Debug: Combined condition for input generation: {combined_condition}")

    for attempt in range(max_attempts):
        # Randomly generate variable values
        inputs = {var: random.randint(-200, 200) for var in variables}

        # Check for duplicates with historical input
        if tuple(inputs.items()) in used_inputs:
            # print(f"Debug: Attempt {attempt + 1}, Generated inputs are duplicate: {inputs}")
            continue

        # Evaluate the logical expression using the generated input
        result = evaluate_expression(combined_condition, inputs)

        if result:
            # If an input is found that satisfies the criteria and does not duplicate
            used_inputs.add(tuple(inputs.items()))
            print(f"Debug: Satisfying inputs found: {inputs}")
            return inputs

    # print("Debug: No satisfying inputs found within the maximum attempts.")
    return None

def simplify_expression(expression):
    """
    Simplify logical expressions and remove redundant negations and redundant conditions.
    """
    # Remove double negatives!! (expr) -> (expr)
    expression = re.sub(r'!\(!\((.*?)\)\)', r'\1', expression)
    expression = re.sub(r'\s+', ' ', expression)  # Remove excess space
    return expression


def repeat_execution_with_ct(java_code, T, D, rounds, input_variables):
    print("\n### Automated Execution ###")
    previous_cts = []  # Store all historical Ct conditions
    used_inputs = set()  # Stores all used inputs

    for round_num in range(1, rounds + 1):
        print(f"\n### Execution Round {round_num} ###")

        # 生成当前逻辑表达式
        logical_expression = generate_logical_expression(T, previous_cts)
        print(f"Current Logical Expression: {logical_expression}")

        # 简化表达式
        logical_expression = simplify_expression(logical_expression)
        generated_inputs={}
        # 执行 Java 程序
        execution_output = run_java_code(java_code)
        if not execution_output:
            print("No output from Java code execution.")
            continue

        # 提取执行路径
        execution_path = parse_execution_path(execution_output)
        print("\nExecution Path:")
        for step in execution_path:
            print(step)

        # 推导 Hoare 逻辑
        derivation, new_d, new_ct = derive_hoare_logic(D, execution_path)
        print("\nHoare Logic Derivation:")
        for step in derivation:
            print(step)

        print(f"\nNew D: {new_d}")
        print(f"\nNew Ct: {new_ct}")

        if new_ct not in previous_cts:
            previous_cts.append(new_ct)

        # 构建新的逻辑表达式并检查可满足性
        negated_d = f"!({new_d})"
        new_logic_expression = f"{T} && {new_ct} && {negated_d}"
        new_logic_expression = simplify_expression(new_logic_expression)
        print(f"\nT && Ct && !D: {new_logic_expression}")

        try:
            preprocessed_input = preprocess_expression(new_logic_expression)
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
                for v in variables.values():
                    print(f"{v} = {model[v]}")
            else:
                print("The expression is unsatisfiable")
                #创建 Result 对象
                result = Result(0,"",parsed_expr)
                json_res = result.to_json()
                print("result:"+json_res)

        except Exception as e:
            print("solver check fail!")
            print("错误信息:", e)

        previous_cts.append(new_ct)
def derive_hoare_logic(specification: str, execution_path: list) -> (list, str, str):
    """
     The modified derive_hoare_logic supports D for complex mathematical expressions and handles single or multiple conditions.
    """
    derivation = []
    current_condition = specification

    # Try to resolve whether D contains "&&"
    if "&&" in specification:
        # The improved regular expression supports complex mathematical expressions nested in parentheses
        d_pattern = r"\((.+?)\)\s*&&\s*\((.+?)\)"
        d_match = re.match(d_pattern, specification)

        if d_match:
            # If the match is successful, the subcondition is extracted
            d1 = d_match.group(1).strip()
            d2 = d_match.group(2).strip()
        else:
            # When the match fails, a warning is thrown, but the entire D continues to be treated as a single condition
            # print("Warning: Unable to parse D as two subconditions. Treating D as a single condition.")
            d1 = specification.strip()
            d2 = None
    else:
        # If D is a single condition
        d1 = specification.strip()
        d2 = None

    # Initializes a subexpression of D
    updated_d1 = d1
    updated_d2 = d2

    for step in reversed(execution_path):
        derivation.append(f"After executing: {step}")

        if "Evaluating if condition" in step:
            condition_match = re.search(r"Evaluating if condition: (.*?) is evaluated as: (.*?)", step)
            if condition_match:
                if_condition = condition_match.group(1).strip()
                current_condition = f"{current_condition} && ({if_condition})"
        # Check whether it is a condition to enter the loop
        if "Entering loop" in step:
            condition_match = re.search(r"Entering loop with condition: (.*?) is evaluated as: true", step)
            if condition_match:
                loop_condition = condition_match.group(1).strip()
                current_condition = f"{current_condition} && ({loop_condition})"

        # Check whether it is a condition for exiting the loop
        elif "Exiting loop" in step:
            condition_match = re.search(r"Exiting loop, condition no longer holds: (.*?) is evaluated as: false", step)
            if condition_match:
                loop_condition = condition_match.group(1).strip()
                current_condition = f"{current_condition} && !({loop_condition})"

        # Check for variable assignment
        elif "current value" in step:
            assignment_match = re.search(r"(.*?) = (.*?), current value of (.*?): (.*?)$", step)
            if assignment_match:
                variable = assignment_match.group(1).strip()
                value = assignment_match.group(2).strip()

                # Update a child of D
                updated_d1 = replace_variables(updated_d1, variable, value)
                if updated_d2 is not None:
                    updated_d2 = replace_variables(updated_d2, variable, value)

                # Update current condition
                current_condition = replace_variables(current_condition, variable, value)

        derivation.append(f"Current Condition: {current_condition}")

    # New D and New Ct were constructed
    if updated_d2 is not None:
        new_d = f"({updated_d1}) && ({updated_d2})"
    else:
        new_d = updated_d1  # If there is no second subcondition, simply return updated_d1

    new_ct = current_condition.replace(new_d, "").strip(" &&")

    return derivation, new_d, new_ct
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
    print(f"Processing SpecUnit JSON: {spec_unit_json}")
    try:
        spec_unit = SpecUnit.from_json(spec_unit_json)
    except json.JSONDecodeError as e:
        print(f"Error decoding JSON: {e}")
    program = spec_unit.program
    T = spec_unit.T
    D = spec_unit.D
    previous_cts = spec_unit.pre_constrains

    #组装 combined_expr
    combined_expr = combind_T_and_preCt(T,previous_cts)

    #运行程序,获得路径输出
    execution_output = run_java_code(program)
    if not execution_output:
        print("No output from Java code execution.")
    #分析路径输出，得到本次执行路径相关的Ct
    execution_path = parse_execution_path(execution_output)
    print("\nExecution Path:")
    for step in execution_path:
        print(step)
    current_ct = get_ct_from_execution_path(execution_path);
    print(f"本次路径对应的_Ct_: {current_ct}")


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

    return ct


# def deal_with_spec_unit_json(spec_unit_json: str):
#     #读取SpecUnit对象
#     spec_unit = None
#     print(f"Processing SpecUnit JSON: {spec_unit_json}")
#     try:
#         spec_unit = SpecUnit.from_json(spec_unit_json)
#     except json.JSONDecodeError as e:
#         print(f"Error decoding JSON: {e}")
#     program = spec_unit.program
#     T = spec_unit.T
#     D = spec_unit.D
#     pre_constrains = spec_unit.pre_constrains
#
#     time_start = time.time()
#     input_variables = {}
#     repeat_execution_with_ct(program, T, D, 1, input_variables)
#     time_end = time.time()
#     # Calculate running time (seconds)
#     execution_time = time_end - time_start
#     execution_time=execution_time
#     print(f"Running time: {execution_time:.6f} seconds")
#     print("Totally verified !")


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
    combined_expr = combind_T_and_preCt(" (num>0) ",["(num > 1)","(num > 2)"])
    print(combined_expr)

def init_files() -> None:
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
    # test_main()
    main()

